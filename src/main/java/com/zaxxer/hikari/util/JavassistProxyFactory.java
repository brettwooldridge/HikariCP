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

package com.zaxxer.hikari.util;

import java.lang.reflect.Array;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.zaxxer.hikari.pool.ProxyCallableStatement;
import com.zaxxer.hikari.pool.ProxyConnection;
import com.zaxxer.hikari.pool.ProxyFactory;
import com.zaxxer.hikari.pool.ProxyPreparedStatement;
import com.zaxxer.hikari.pool.ProxyResultSet;
import com.zaxxer.hikari.pool.ProxyStatement;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.LoaderClassPath;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.bytecode.ClassFile;

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
   private static ClassPool classPool;

   public static void main(String... args)
   {
      classPool = new ClassPool();
      classPool.importPackage("java.sql");
      classPool.appendClassPath(new LoaderClassPath(JavassistProxyFactory.class.getClassLoader()));

      try {
         // Cast is not needed for these
         String methodBody = "{ try { return delegate.method($$); } catch (SQLException e) { throw checkException(e); } }";
         generateProxyClass(Connection.class, ProxyConnection.class.getName(), methodBody);
         generateProxyClass(Statement.class, ProxyStatement.class.getName(), methodBody);
         generateProxyClass(ResultSet.class, ProxyResultSet.class.getName(), methodBody);

         // For these we have to cast the delegate
         methodBody = "{ try { return ((cast) delegate).method($$); } catch (SQLException e) { throw checkException(e); } }";
         generateProxyClass(PreparedStatement.class, ProxyPreparedStatement.class.getName(), methodBody);
         generateProxyClass(CallableStatement.class, ProxyCallableStatement.class.getName(), methodBody);

         modifyProxyFactory();
      }
      catch (Exception e) {
         throw new RuntimeException(e);
      }
   }

   private static void modifyProxyFactory() throws Exception
   {
      System.out.println("Generating method bodies for com.zaxxer.hikari.proxy.ProxyFactory");

      String packageName = ProxyConnection.class.getPackage().getName();
      CtClass proxyCt = classPool.getCtClass("com.zaxxer.hikari.pool.ProxyFactory");
      for (CtMethod method : proxyCt.getMethods()) {
         switch (method.getName()) {
         case "getProxyConnection":
            method.setBody("{return new " + packageName + ".HikariProxyConnection($$);}");
            break;
         case "getProxyStatement":
            method.setBody("{return new " + packageName + ".HikariProxyStatement($$);}");
            break;
         case "getProxyPreparedStatement":
            method.setBody("{return new " + packageName + ".HikariProxyPreparedStatement($$);}");
            break;
         case "getProxyCallableStatement":
            method.setBody("{return new " + packageName + ".HikariProxyCallableStatement($$);}");
            break;
         case "getProxyResultSet":
            method.setBody("{return new " + packageName + ".HikariProxyResultSet($$);}");
            break;
         }
      }

      proxyCt.writeFile("target/classes");
   }

   /**
    *  Generate Javassist Proxy Classes
    */
   private static <T> void generateProxyClass(Class<T> primaryInterface, String superClassName, String methodBody) throws Exception
   {
      String newClassName = superClassName.replaceAll("(.+)\\.(\\w+)", "$1.Hikari$2");

      CtClass superCt = classPool.getCtClass(superClassName);
      CtClass targetCt = classPool.makeClass(newClassName, superCt);
      targetCt.setModifiers(Modifier.FINAL);

      System.out.println("Generating " + newClassName);

      targetCt.setModifiers(Modifier.PUBLIC);

      // Make a set of method signatures we inherit implementation for, so we don't generate delegates for these
      Set<String> superSigs = new HashSet<>();
      for (CtMethod method : superCt.getMethods()) {
         if ((method.getModifiers() & Modifier.FINAL) == Modifier.FINAL) {
            superSigs.add(method.getName() + method.getSignature());
         }
      }

      Set<String> methods = new HashSet<>();
      Set<Class<?>> interfaces = getAllInterfaces(primaryInterface);
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

            String modifiedBody = methodBody;

            // If the super-Proxy has concrete methods (non-abstract), transform the call into a simple super.method() call
            CtMethod superMethod = superCt.getMethod(intfMethod.getName(), intfMethod.getSignature());
            if ((superMethod.getModifiers() & Modifier.ABSTRACT) != Modifier.ABSTRACT && !isDefaultMethod(intf, intfCt, intfMethod)) {
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

      targetCt.getClassFile().setMajorVersion(ClassFile.JAVA_7);
      targetCt.writeFile("target/classes");
   }

   private static boolean isThrowsSqlException(CtMethod method)
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

   private static boolean isDefaultMethod(Class<?> intf, CtClass intfCt, CtMethod intfMethod) throws Exception
   {
      List<Class<?>> paramTypes = new ArrayList<>();

      for (CtClass pt : intfMethod.getParameterTypes()) {
         paramTypes.add(toJavaClass(pt));
      }

      return intf.getDeclaredMethod(intfMethod.getName(), paramTypes.toArray(new Class[paramTypes.size()])).toString().contains("default ");
   }

   private static Set<Class<?>> getAllInterfaces(Class<?> clazz)
   {
      Set<Class<?>> interfaces = new HashSet<>();
      for (Class<?> intf : Arrays.asList(clazz.getInterfaces())) {
         if (intf.getInterfaces().length > 0) {
            interfaces.addAll(getAllInterfaces(intf));
         }
         interfaces.add(intf);
      }
      if (clazz.getSuperclass() != null) {
         interfaces.addAll(getAllInterfaces(clazz.getSuperclass()));
      }

      if (clazz.isInterface()) {
         interfaces.add(clazz);
      }

      return interfaces;
   }

   private static Class<?> toJavaClass(CtClass cls) throws Exception
   {
      if (cls.getName().endsWith("[]")) {
         return Array.newInstance(toJavaClass(cls.getName().replace("[]", "")), 0).getClass();
      }
      else {
         return toJavaClass(cls.getName());
      }
   }

   private static Class<?> toJavaClass(String cn) throws Exception
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
