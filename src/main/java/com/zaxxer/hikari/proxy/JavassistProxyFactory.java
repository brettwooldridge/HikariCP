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

import java.lang.reflect.Array;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.LoaderClassPath;
import javassist.Modifier;
import javassist.NotFoundException;

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
   private final ClassPool classPool;

   static {
      ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
      try {
         Thread.currentThread().setContextClassLoader(JavassistProxyFactory.class.getClassLoader());

         JavassistProxyFactory proxyFactoryFactory = new JavassistProxyFactory();
         proxyFactoryFactory.modifyProxyFactory();
      }
      catch (Exception e) {
         throw new RuntimeException("Fatal exception during proxy generation", e);
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

   private JavassistProxyFactory() throws Exception
   {
      classPool = new ClassPool();
      classPool.importPackage("java.sql");
      classPool.appendClassPath(new LoaderClassPath(this.getClass().getClassLoader()));

      // Cast is not needed for these
      String methodBody = "{ try { return delegate.method($$); } catch (SQLException e) { throw checkException(e); } }";
      generateProxyClass(Connection.class, ConnectionProxy.class, methodBody);
      generateProxyClass(Statement.class, StatementProxy.class, methodBody);
      generateProxyClass(ResultSet.class, ResultSetProxy.class, methodBody);

      // For these we have to cast the delegate
      methodBody = "{ try { return ((cast) delegate).method($$); } catch (SQLException e) { throw checkException(e); } }";
      generateProxyClass(PreparedStatement.class, PreparedStatementProxy.class, methodBody);
      generateProxyClass(CallableStatement.class, CallableStatementProxy.class, methodBody);
   }

   private void modifyProxyFactory() throws Exception
   {
      String packageName = JavassistProxyFactory.class.getPackage().getName();
      CtClass proxyCt = classPool.getCtClass("com.zaxxer.hikari.proxy.ProxyFactory");
      for (CtMethod method : proxyCt.getMethods()) {
         switch (method.getName()) {
         case "getProxyConnection":
            method.setBody("{return new " + packageName + ".ConnectionJavassistProxy($$);}");
            break;
         case "getProxyStatement":
            method.setBody("{return new " + packageName + ".StatementJavassistProxy($$);}");
            break;
         case "getProxyPreparedStatement":
            method.setBody("{return new " + packageName + ".PreparedStatementJavassistProxy($$);}");
            break;
         case "getProxyCallableStatement":
            method.setBody("{return new " + packageName + ".CallableStatementJavassistProxy($$);}");
            break;
         case "getProxyResultSet":
            method.setBody("{return new " + packageName + ".ResultSetJavassistProxy($$);}");
            break;
         }
      }

      proxyCt.toClass(classPool.getClassLoader(), getClass().getProtectionDomain());
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
      Set<String> superSigs = new HashSet<>();
      for (CtMethod method : superClassCt.getMethods()) {
         if ((method.getModifiers() & Modifier.FINAL) == Modifier.FINAL) {
            superSigs.add(method.getName() + method.getSignature());
         }
      }

      Set<String> methods = new HashSet<>();
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

            // Ignore default methods (only for Jre8 or later)
            if (isDefaultMethod(intf, intfCt, intfMethod)) {
               continue;
            }

            // Track what methods we've added
            methods.add(signature);

            // Clone the method we want to inject into
            CtMethod method = CtNewMethod.copy(intfMethod, targetCt, null);

            String modifiedBody = methodBody;

            // If the super-Proxy has concrete methods (non-abstract), transform the call into a simple super.method() call
            CtMethod superMethod = superClassCt.getMethod(intfMethod.getName(), intfMethod.getSignature());
            if ((superMethod.getModifiers() & Modifier.ABSTRACT) != Modifier.ABSTRACT) {
               modifiedBody = modifiedBody.replace("((cast) ", "");
               modifiedBody = modifiedBody.replace("delegate", "super");
               modifiedBody = modifiedBody.replace("super)", "super");
            }

            modifiedBody = modifiedBody.replace("cast", primaryInterface.getName());

            // Generate a method that simply invokes the same method on the delegate
            if (isThrowsSqlException(intfMethod)) {
               modifiedBody = modifiedBody.replace("method", method.getName());
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

      return targetCt.toClass(classPool.getClassLoader(), getClass().getProtectionDomain());
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

   private boolean isDefaultMethod(Class<?> intf, CtClass intfCt, CtMethod intfMethod) throws Exception
   {
      List<Class<?>> paramTypes = new ArrayList<>();

      for (CtClass pt : intfMethod.getParameterTypes()) {
         paramTypes.add(toJavaClass(pt));
      }

      return intf.getDeclaredMethod(intfMethod.getName(), paramTypes.toArray(new Class[paramTypes.size()])).toString().contains("default ");
   }

   private Class<?> toJavaClass(CtClass cls) throws Exception
   {
      if (cls.getName().endsWith("[]")) {
         return Array.newInstance(toJavaClass(cls.getName().replace("[]", "")), 0).getClass();
      }
      else {
         return toJavaClass(cls.getName());
      }
   }

   private Class<?> toJavaClass(String cn) throws Exception
   {
      switch (cn) {
      case "int":
         return int.class;
      case "long":
         return long.class;
      case "short":
         return short.class;
      case "byte":
         return byte.class;
      case "float":
         return float.class;
      case "double":
         return double.class;
      case "boolean":
         return boolean.class;
      case "char":
         return char.class;
      case "void":
         return void.class;
      default:
         return Class.forName(cn);
      }
   }
}
