/*
 * Copyright (C) 2013, 2014 Brett Wooldridge
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zaxxer.hikari.proxy;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.LoaderClassPath;
import javassist.Modifier;
import javassist.NotFoundException;

import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.util.ClassLoaderUtils;

/**
 * This class generates the proxy objects for {@link Connection}, {@link Statement},
 * {@link PreparedStatement}, and {@link CallableStatement}.  Additionally it injects
 * method bodies into the {@link ProxyFactory} class methods that can instantiate
 * instances of the generated proxies.
 *
 * @author Brett Wooldridge
 */
public final class JavassistProxyFactory
{
   private ClassPool classPool;

   static {
      ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
      try {
         Thread.currentThread().setContextClassLoader(JavassistProxyFactory.class.getClassLoader());

         JavassistProxyFactory proxyFactoryFactory = new JavassistProxyFactory();
         proxyFactoryFactory.modifyProxyFactory();
      }
      catch (Exception e) {
         LoggerFactory.getLogger(JavassistProxyFactory.class).error("Fatal exception during proxy generation", e);
         throw new RuntimeException(e);
      }
      finally {
         Thread.currentThread().setContextClassLoader(contextClassLoader);
      }
   }

   /**
    * Simply invoking this method causes the initialization of this class.  All work
    * by this class is performed in static initialization.
    */
   public static void initialize()
   {
      // no-op
   }

   private JavassistProxyFactory()
   {
      classPool = new ClassPool();
      classPool.importPackage("java.sql");
      classPool.appendClassPath(new LoaderClassPath(this.getClass().getClassLoader()));

      try {
         // Connection is special, it has a checkClosed() call at the beginning
         String methodBody = "{ checkClosed(); try { return delegate.method($$); } catch (SQLException e) { checkException(e); throw e;} }";
         generateProxyClass(Connection.class, ConnectionProxy.class, methodBody);

         // Cast is not needed for these
         methodBody = "{ try { return delegate.method($$); } catch (SQLException e) { checkException(e); throw e;} }";
         generateProxyClass(Statement.class, StatementProxy.class, methodBody);

         // For these we have to cast the delegate
         methodBody = "{ try { return ((cast) delegate).method($$); } catch (SQLException e) { checkException(e); throw e;} }";
         generateProxyClass(PreparedStatement.class, PreparedStatementProxy.class, methodBody);
         generateProxyClass(CallableStatement.class, CallableStatementProxy.class, methodBody);
      }
      catch (Exception e) {
         throw new RuntimeException(e);
      }
   }

   private void modifyProxyFactory() throws Exception
   {
      String packageName = JavassistProxyFactory.class.getPackage().getName();
      CtClass proxyCt = classPool.getCtClass("com.zaxxer.hikari.proxy.ProxyFactory");
      for (CtMethod method : proxyCt.getMethods()) {
         String methodName = method.getName();
         if ("getProxyConnection".equals(methodName)) {
            method.setBody("{return new " + packageName + ".ConnectionJavassistProxy($$);}");
         }
         else if ("getProxyStatement".equals(methodName)) {
            method.setBody("{return new " + packageName + ".StatementJavassistProxy($$);}");
         }
         else if ("getProxyPreparedStatement".equals(methodName)) {
            method.setBody("{return new " + packageName + ".PreparedStatementJavassistProxy($$);}");
         }
         else if ("getProxyCallableStatement".equals(methodName)) {
            method.setBody("{return new " + packageName + ".CallableStatementJavassistProxy($$);}");
         }
      }

      proxyCt.toClass(classPool.getClassLoader(), null);
   }

   /**
    *  Generate Javassist Proxy Classes
    */
   @SuppressWarnings("unchecked")
   private <T> Class<T> generateProxyClass(Class<T> primaryInterface, Class<?> superClass, String methodBody) throws Exception
   {
      // Make a new class that extends one of the JavaProxy classes (ie. superClass); use the name to XxxJavassistProxy instead of XxxProxy
      String superClassName = superClass.getName();
      CtClass superClassCt = classPool.getCtClass(superClassName);
      CtClass targetCt = classPool.makeClass(superClassName.replace("Proxy", "JavassistProxy"), superClassCt);
      targetCt.setModifiers(Modifier.FINAL);

      // Make a set of method signatures we inherit implementation for, so we don't generate delegates for these
      Set<String> superSigs = new HashSet<String>();
      for (CtMethod method : superClassCt.getMethods()) {
         if ((method.getModifiers() & Modifier.ABSTRACT) != Modifier.ABSTRACT) {
            superSigs.add(method.getName() + method.getSignature());
         }
      }

      methodBody = methodBody.replace("cast", primaryInterface.getName());

      Set<String> methods = new HashSet<String>();
      Set<Class<?>> interfaces = ClassLoaderUtils.getAllInterfaces(primaryInterface);
      for (Class<?> intf : interfaces) {
         CtClass intfCt = classPool.getCtClass(intf.getName());
         targetCt.addInterface(intfCt);
         for (CtMethod intfMethod : intfCt.getDeclaredMethods()) {
            final String signature = intfMethod.getName() + intfMethod.getSignature();

            // don't generate delegates for methods we override
            if (superSigs.contains(signature)) {
               continue;
            }

            // Ignore already added methods that come from other interfaces
            if (methods.contains(signature)) {
               continue;
            }

            // Track what methods we've added
            methods.add(signature);

            // Clone the method we want to inject into
            CtMethod method = CtNewMethod.copy(intfMethod, targetCt, null);

            // Generate a method that simply invokes the same method on the delegate
            String modifiedBody;
            if (isThrowsSqlException(intfMethod)) {
               modifiedBody = methodBody.replace("method", method.getName());
            }
            else {
               modifiedBody = "{ return ((cast) delegate).method($$); }".replace("method", method.getName()).replace("cast", primaryInterface.getName());
            }

            if (method.getReturnType() == CtClass.voidType) {
               modifiedBody = modifiedBody.replace("return", "");
            }

            method.setBody(modifiedBody);
            targetCt.addMethod(method);
         }
      }

      if (LoggerFactory.getLogger(getClass()).isDebugEnabled()) {
         targetCt.debugWriteFile(System.getProperty("java.io.tmpdir"));
      }

      return targetCt.toClass(classPool.getClassLoader(), null);
   }

   private boolean isThrowsSqlException(CtMethod method)
   {
      try {
         for (CtClass clazz : method.getExceptionTypes()) {
            if (clazz.getSimpleName().equals("SQLException")) {
               return true;
            }
         }
      }
      catch (NotFoundException e) {
         // fall thru
      }

      return false;
   }
}
