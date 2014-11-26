/*
 * Copyright (C) 2013 Brett Wooldridge
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

package com.zaxxer.hikari.metrics;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.LoaderClassPath;
import javassist.Modifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used to validate that Codahale metrics is available in the class
 * path, or if not to generate a fake "shim" that avoids ClassNotFound exceptions
 * in code that depends on (but does not use if not present) Codahale metrics.
 *
 * @author Brett Wooldridge
 */
public class CodaHaleShim
{
   private static final Logger LOGGER = LoggerFactory.getLogger(CodaHaleShim.class);

   private CodaHaleShim()
   {
      // empty
   }

   /**
    * Simply invoking this method causes the initialization of this class.  All work
    * by this class is performed in static initialization.
    */
   public static void initialize()
   {
      CodaHaleShim codaHaleShim = new CodaHaleShim();
      codaHaleShim.loadOrShimCodahale();
   }

   /**
    * 
    */
   private void loadOrShimCodahale()
   {
      ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();

      try {
         CodaHaleShim.class.getClassLoader().loadClass("com.codahale.metrics.MetricRegistry");
      }
      catch (ClassNotFoundException e) {
         // OK, it is not present, we have to generate a shim
         LOGGER.debug("com.codahale.metrics.MetricRegistry not found, generating stub");

         ClassPool classPool = new ClassPool();
         classPool.appendClassPath(new LoaderClassPath(this.getClass().getClassLoader()));

         try {
            CtClass targetCt = classPool.makeClass("com.codahale.metrics.MetricRegistry");
            targetCt.setModifiers(Modifier.PUBLIC | Modifier.FINAL);

            CtConstructor constructorCt = new CtConstructor(null, targetCt);
            constructorCt.setModifiers(Modifier.PUBLIC);
            constructorCt.setBody("{ throw new RuntimeException(\"HikariCP Codahale shim says: Codahale metrics library is required but was not found in the classpath\"); }");
            targetCt.addConstructor(constructorCt);

            targetCt.toClass(classPool.getClassLoader(), getClass().getProtectionDomain());
         }
         catch (CannotCompileException cce) {
            LOGGER.error("Cannot generate CodaHale metrics shim", cce);
            throw new RuntimeException(cce);
         }
      }
      finally {
         Thread.currentThread().setContextClassLoader(contextClassLoader);
      }
   }
}
