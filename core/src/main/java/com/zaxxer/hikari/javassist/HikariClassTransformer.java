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

package com.zaxxer.hikari.javassist;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.HashSet;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewConstructor;
import javassist.CtNewMethod;
import javassist.LoaderClassPath;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;
import javassist.bytecode.ConstPool;
import javassist.bytecode.annotation.Annotation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.proxy.CallableStatementProxy;
import com.zaxxer.hikari.proxy.ConnectionProxy;
import com.zaxxer.hikari.proxy.IHikariConnectionProxy;
import com.zaxxer.hikari.proxy.IHikariResultSetProxy;
import com.zaxxer.hikari.proxy.IHikariStatementProxy;
import com.zaxxer.hikari.proxy.PreparedStatementProxy;
import com.zaxxer.hikari.proxy.ResultSetProxy;
import com.zaxxer.hikari.proxy.StatementProxy;

/**
 *
 * @author Brett Wooldridge
 */
public class HikariClassTransformer implements ClassFileTransformer
{
    private final Logger LOGGER;

    public static final int UNDEFINED = 0;
    public static final int CONNECTION = 1;
    public static final int STATEMENT = 2;
    public static final int PREPARED_STATEMENT = 3;
    public static final int CALLABLE_STATEMENT = 4;
    public static final int RESULTSET = 5;
    public static final int CONNECTION_SUBCLASS = 6;
    public static final int STATEMENT_SUBCLASS = 7;
    public static final int PREPARED_STATEMENT_SUBCLASS = 8;
    public static final int CALLABLE_STATEMENT_SUBCLASS = 9;
    public static final int RESULTSET_SUBCLASS = 10;

    private static ClassPool classPool;

    private volatile boolean agentFailed;

    private volatile HashSet<String> scanClasses;
    private int classType;

    /**
     * Private constructor.
     * @param shadedCodexMapping 
     * 
     * @param sniffPackage the package name used to filter only classes we are interested in
     */
    public HikariClassTransformer()
    {
        LOGGER = LoggerFactory.getLogger(HikariClassTransformer.class);
    }

    public void setScanClass(HashSet<String> scanClasses, int classType)
    {
        this.scanClasses = new HashSet<>();
        for (String scanClass : scanClasses)
        {
            this.scanClasses.add(scanClass.replace('.', '/'));
        }
                
        this.classType = classType;
    }

    /** {@inheritDoc} */
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer)
            throws IllegalClassFormatException
    {
        if (classType == UNDEFINED || !scanClasses.contains(className))
        {
            return classfileBuffer;
        }

        if (classPool == null)
        {
            classPool = new ClassPool();
            classPool.appendClassPath(new LoaderClassPath(loader));
            classPool.appendClassPath(new LoaderClassPath(this.getClass().getClassLoader()));
        }

        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try
        {
            Thread.currentThread().setContextClassLoader(classPool.getClassLoader());

            ClassFile classFile = new ClassFile(new DataInputStream(new ByteArrayInputStream(classfileBuffer)));

            LOGGER.info("Instrumenting class {}", className);

            switch (classType)
            {
            case CONNECTION:
                return transformBaseConnection(classFile);
            case CONNECTION_SUBCLASS:
                return transformConnectionSubclass(classFile);
            case STATEMENT:
                return transformBaseClass(classFile, StatementProxy.class, IHikariStatementProxy.class);
            case STATEMENT_SUBCLASS:
                return transformClass(classFile, StatementProxy.class);
            case PREPARED_STATEMENT:
                return transformBaseClass(classFile, PreparedStatementProxy.class, IHikariStatementProxy.class);
            case PREPARED_STATEMENT_SUBCLASS:
                return transformClass(classFile, PreparedStatementProxy.class);
            case CALLABLE_STATEMENT:
                return transformBaseClass(classFile, CallableStatementProxy.class, IHikariStatementProxy.class);
            case CALLABLE_STATEMENT_SUBCLASS:
                return transformClass(classFile, CallableStatementProxy.class);
            case RESULTSET:
                return transformBaseClass(classFile, ResultSetProxy.class, IHikariResultSetProxy.class);
            case RESULTSET_SUBCLASS:
                return transformClass(classFile, ResultSetProxy.class);
            default:
                // None of the interfaces we care about were found, so just return the class file buffer
                return classfileBuffer;
            }
        }
        catch (Exception e)
        {
            agentFailed = true;
            LOGGER.error("Error transforming class {}", className, e);
            return classfileBuffer;
        }
        finally
        {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
            LOGGER.debug("--------------------------------------------------------------------------");
        }
    }

    public boolean isAgentFailed()
    {
        return agentFailed;
    }

    /**
     * @param classFile
     */
    private byte[] transformBaseConnection(ClassFile classFile) throws Exception
    {
        String className = classFile.getName();
        CtClass target = classPool.getCtClass(className);

        CtClass intf = classPool.get(IHikariConnectionProxy.class.getName());
        target.addInterface(intf);
        LOGGER.debug("Added interface {} to {}", intf.getName(), className);

        CtClass proxy = classPool.get(ConnectionProxy.class.getName());

        copyFields(proxy, target);
        copyMethods(proxy, target, classFile);

        for (CtConstructor constructor : target.getDeclaredConstructors())
        {
            constructor.insertBeforeBody("__init();");
        }
        
        mergeClassInitializers(proxy, target, classFile);

        return transformConnectionSubclass(classFile);
    }

    /**
     * @param classFile
     */
    private byte[] transformConnectionSubclass(ClassFile classFile) throws Exception
    {
        String className = classFile.getName();
        CtClass target = classPool.getCtClass(className);
        CtClass proxy = classPool.get(ConnectionProxy.class.getName());

        overrideMethods(proxy, target, classFile);
        injectTryCatch(target);
        specialConnectionInjectCloseCheck(target);

        for (CtConstructor constructor : target.getDeclaredConstructors())
        {
            constructor.insertBeforeBody("__init();");
        }

        return target.toBytecode();
    }

    /**
     * @param classFile
     */
    private byte[] transformBaseClass(ClassFile classFile, Class<?> proxyClass, Class<?> interfase) throws Exception
    {
        String className = classFile.getName();
        CtClass target = classPool.getCtClass(className);

        CtClass intf = classPool.get(interfase.getName());
        target.addInterface(intf);
        LOGGER.debug("Added interface {} to {}", intf.getName(), className);

        CtClass proxy = classPool.get(proxyClass.getName());

        copyFields(proxy, target);
        copyMethods(proxy, target, classFile);
        mergeClassInitializers(proxy, target, classFile);

        return transformClass(classFile, proxyClass);
    }

    /**
     * @param classFile
     */
    private byte[] transformClass(ClassFile classFile, Class<?> proxyClass) throws Exception
    {
        String className = classFile.getName();
        CtClass target = classPool.getCtClass(className);

        CtClass proxy = classPool.get(proxyClass.getName());

        overrideMethods(proxy, target, classFile);
        injectTryCatch(target);

        return target.toBytecode();
    }

    private void copyFields(CtClass srcClass, CtClass targetClass) throws Exception
    {
        HashSet<CtField> srcFields = new HashSet<CtField>();
        srcFields.addAll(Arrays.asList(srcClass.getDeclaredFields()));
        srcFields.addAll(Arrays.asList(srcClass.getFields()));
        for (CtField field : srcFields)
        {
            if (field.getAnnotation(HikariInject.class) == null)
            {
                continue;
            }

            CtField copy = new CtField(field.getType(), field.getName(), targetClass);
            copy.setModifiers(field.getModifiers());
            targetClass.addField(copy);
            LOGGER.debug("Copied field {}.{} to {}", field.getDeclaringClass().getSimpleName(), field.getName(), targetClass.getSimpleName());
        }
    }

    private void copyMethods(CtClass srcClass, CtClass targetClass, ClassFile targetClassFile) throws Exception
    {
        ConstPool constPool = targetClassFile.getConstPool();

        for (CtMethod method : srcClass.getMethods())
        {
            if (method.getAnnotation(HikariInject.class) == null)
            {
                continue;
            }

            CtMethod copy = CtNewMethod.copy(method, targetClass, null);
            AnnotationsAttribute attr = new AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag);
            Annotation annotation = new Annotation(HikariInject.class.getName(), constPool);
            attr.setAnnotation(annotation);
            copy.getMethodInfo().addAttribute(attr);
            targetClass.addMethod(copy);
            LOGGER.debug("Copied method {}.{} to {}", method.getDeclaringClass().getSimpleName(), method.getName(), targetClass.getSimpleName());
        }
    }

    private void overrideMethods(CtClass srcClass, CtClass targetClass, ClassFile targetClassFile) throws Exception
    {
        ConstPool constPool = targetClassFile.getConstPool();
        AnnotationsAttribute attr = new AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag);
        Annotation annotation = new Annotation(HikariOverride.class.getName(), constPool);
        attr.setAnnotation(annotation);

        for (CtMethod method : srcClass.getMethods())
        {
            if (method.getAnnotation(HikariOverride.class) == null)
            {
                continue;
            }

            if ((targetClass.getModifiers() & Modifier.ABSTRACT) == Modifier.ABSTRACT)
            {
            	continue;
            }

            try
            {
                CtMethod destMethod = targetClass.getDeclaredMethod(method.getName(), method.getParameterTypes());
                LOGGER.debug("Rename method {}{} to __{}", destMethod.getName(), destMethod.getSignature(), destMethod.getName());
                destMethod.setName("__" + destMethod.getName());
                destMethod.getMethodInfo().addAttribute(attr);

                CtMethod copy = CtNewMethod.copy(method, targetClass, null);
                copy.getMethodInfo().addAttribute(attr);
                targetClass.addMethod(copy);
                LOGGER.debug("Override method {}.{} in {}", method.getDeclaringClass().getSimpleName(), method.getName(), targetClass.getSimpleName());
            }
            catch (NotFoundException nfe)
            {
            	// fall thru
            }
        }
    }

    private void mergeClassInitializers(CtClass srcClass, CtClass targetClass, ClassFile targetClassFile) throws Exception
    {
        CtConstructor srcInitializer = srcClass.getClassInitializer();
        if (srcInitializer == null)
        {
            return;
        }

        CtConstructor destInitializer = targetClass.getClassInitializer();
        if (destInitializer == null && srcInitializer != null)
        {
            CtConstructor copy = CtNewConstructor.copy(srcInitializer, targetClass, null);
            targetClass.addConstructor(copy);
            CtMethod __static = CtNewMethod.make(Modifier.STATIC, CtClass.voidType, "__static", null, null, "{}", targetClass);
            targetClass.addMethod(__static);
            LOGGER.debug("Copied static initializer of {} to {}", srcClass.getSimpleName(), targetClass.getSimpleName());
        }
        else
        {
            CtMethod method = destInitializer.toMethod("__static", targetClass);
            targetClass.addMethod(method);
            targetClass.removeConstructor(destInitializer);
            LOGGER.debug("Move static initializer of {}", targetClass.getSimpleName());
            // mergeClassInitializers(srcClass, targetClass, targetClassFile);
            CtConstructor copy = CtNewConstructor.copy(srcInitializer, targetClass, null);
            targetClass.addConstructor(copy);
            LOGGER.debug("Copied static initializer of {} to {}", srcClass.getSimpleName(), targetClass.getSimpleName());
        }
    }

    private void injectTryCatch(CtClass targetClass) throws Exception
    {
        for (CtMethod method : targetClass.getDeclaredMethods())
        {
            if ((method.getModifiers() & Modifier.PUBLIC) != Modifier.PUBLIC ||  // only public methods
                (method.getModifiers() & Modifier.STATIC) == Modifier.STATIC ||  // no static methods
                method.getAnnotation(HikariInject.class) != null ||
                method.getAnnotation(HikariOverride.class) != null) // ignore methods we've injected, they already try..catch
            {
                continue;
            }

            if (method.getMethodInfo().getCodeAttribute() == null)
            {
                continue;
            }

            for (CtClass exception : method.getExceptionTypes())
            {
                if ("java.sql.SQLException".equals(exception.getName()))         // only add try..catch to methods throwing SQLException
                {
                    LOGGER.debug("Injecting try..catch into {}{}", method.getName(), method.getSignature());
                    method.addCatch("throw _checkException($e);", exception);
                    break;
                }
            }
        }
    }

    private void specialConnectionInjectCloseCheck(CtClass targetClass) throws Exception
    {
        for (CtMethod method : targetClass.getDeclaredMethods())
        {
            if ((method.getModifiers() & Modifier.PUBLIC) != Modifier.PUBLIC ||  // only public methods
                (method.getModifiers() & Modifier.STATIC) == Modifier.STATIC ||
                method.getAnnotation(HikariInject.class) != null ||
                method.getAnnotation(HikariOverride.class) != null)  // ignore methods we've injected, they already try..catch
            {
                continue;
            }

            if (method.getMethodInfo().getCodeAttribute() == null)
            {
                continue;
            }

            for (CtClass exception : method.getExceptionTypes())
            {
                if ("java.sql.SQLException".equals(exception.getName()))         // only add check to methods throwing SQLException
                {
                    method.insertBefore("if (_isClosed) { throw new java.sql.SQLException(\"Connection is closed\"); }");
                    break;
                }
            }
        }
    }
}
