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
import java.lang.instrument.Instrumentation;
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
import javassist.Modifier;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;
import javassist.bytecode.ConstPool;
import javassist.bytecode.annotation.Annotation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Brett Wooldridge
 */
public class HikariClassTransformer implements ClassFileTransformer
{
    private static final Logger LOGGER = LoggerFactory.getLogger(HikariClassTransformer.class);

    private static Instrumentation ourInstrumentation;
    private static HikariClassTransformer transformer;
    private static ClassPool classPool;

    private String sniffPackage;

    private volatile boolean agentFailed;

    /**
     * Private constructor.
     * 
     * @param sniffPackage the package name used to filter only classes we are interested in
     */
    private HikariClassTransformer(String sniffPackage)
    {
        this.sniffPackage = sniffPackage;
        HikariClassTransformer.transformer = this;
    }

    /**
     * The method that is called when VirtualMachine.loadAgent() is invoked to register our
     * class transformer.
     *
     * @param agentArgs arguments to pass to the agent
     * @param inst the virtual machine Instrumentation instance used to register our transformer 
     */
    public static void agentmain(String agentArgs, Instrumentation instrumentation)
    {
        ourInstrumentation = instrumentation;

        ClassPool defaultPool = ClassPool.getDefault();
        classPool = new ClassPool(defaultPool);
        classPool.importPackage("java.sql");
        classPool.childFirstLookup = true;

        ourInstrumentation.addTransformer(new HikariClassTransformer(agentArgs), false);
    }

    /** {@inheritDoc} */
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer)
            throws IllegalClassFormatException
    {
        if (!className.startsWith(sniffPackage))
        {
            return classfileBuffer;
        }

        try
        {
            ClassFile classFile = new ClassFile(new DataInputStream(new ByteArrayInputStream(classfileBuffer)));
            if (classFile.isInterface())
            {
                return classfileBuffer;
            }

            for (String iface : classFile.getInterfaces())
            {
                if (!iface.startsWith("java.sql"))
                {
                    continue;
                }

                if (iface.equals("java.sql.Connection"))
                {
                    return transformConnection(classFile);
                }
                else if (iface.equals("java.sql.PreparedStatement"))
                {
                    return transformClass(classFile, "com.zaxxer.hikari.proxy.PreparedStatementProxy", "com.zaxxer.hikari.proxy.IHikariStatementProxy");
                }
                else if (iface.equals("java.sql.CallableStatement"))
                {
                    return transformClass(classFile, "com.zaxxer.hikari.proxy.CallableStatementProxy", "com.zaxxer.hikari.proxy.IHikariStatementProxy");
                }
                else if (iface.equals("java.sql.Statement"))
                {
                    return transformClass(classFile, "com.zaxxer.hikari.proxy.StatementProxy", "com.zaxxer.hikari.proxy.IHikariStatementProxy");
                }
                else if (iface.equals("java.sql.ResultSet"))
                {
                    return transformClass(classFile, "com.zaxxer.hikari.proxy.ResultSetProxy", "com.zaxxer.hikari.proxy.IHikariResultSetProxy");
                }
            }

            // None of the interfaces we care about were found, so just return the class file buffer
            return classfileBuffer;
        }
        catch (Exception e)
        {
            agentFailed = true;
            LOGGER.error("Error transforming class {}", className, e);
            return classfileBuffer;
        }
    }

    public boolean isAgentFailed()
    {
        return agentFailed;
    }

    /**
     * @param classFile
     */
    private byte[] transformConnection(ClassFile classFile) throws Exception
    {
        String className = classFile.getName();
        CtClass target = classPool.getCtClass(className);

        CtClass intf = classPool.get("com.zaxxer.hikari.proxy.IHikariConnectionProxy");
        target.addInterface(intf);
        LOGGER.debug("Added interface {} to {}", intf.getName(), className);

        CtClass proxy = classPool.get("com.zaxxer.hikari.proxy.ConnectionProxy");

        copyFields(proxy, target);
        copyMethods(proxy, target, classFile);
        mergeClassInitializers(proxy, target, classFile);
        specialConnectionInjectCloseCheck(target);
        injectTryCatch(target);

        for (CtConstructor constructor : target.getConstructors())
        {
            constructor.insertAfter("__init();");
        }

        return target.toBytecode();
    }

    /**
     * @param classFile
     */
    private byte[] transformClass(ClassFile classFile, String proxyClassName, String intfName) throws Exception
    {
        String className = classFile.getName();
        CtClass target = classPool.getCtClass(className);

        CtClass intf = classPool.get(intfName);
        target.addInterface(intf);
        LOGGER.debug("Added interface {} to {}", intf.getName(), className);

        CtClass proxy = classPool.get(proxyClassName);

        copyFields(proxy, target);
        copyMethods(proxy, target, classFile);
        mergeClassInitializers(proxy, target, classFile);
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
                LOGGER.debug("Skipped field {}", field.getName());
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
        CtMethod[] destMethods = targetClass.getMethods();
        ConstPool constPool = targetClassFile.getConstPool();

        for (CtMethod method : srcClass.getDeclaredMethods())
        {
            if (method.getAnnotation(HikariInject.class) == null)
            {
                LOGGER.debug("Skipped method {}", method.getName());
                continue;
            }

            if (targetClassFile.getMethod(method.getName()) != null)  // maybe we have a name collision
            {
                String signature = method.getSignature();
                for (CtMethod destMethod : destMethods)
                {
                    if (destMethod.getName().equals(method.getName()) && destMethod.getSignature().equals(signature))
                    {
                        LOGGER.debug("Rename method {}.{} to __{}", targetClass.getSimpleName(), destMethod.getName(), destMethod.getName());
                        destMethod.setName("__" + destMethod.getName());
                        break;
                    }
                }
            }

            CtMethod copy = CtNewMethod.copy(method, targetClass, null);
            AnnotationsAttribute attr = new AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag);
            Annotation annotation = new Annotation("com.zaxxer.hikari.javassist.HikariInject", constPool);
            attr.setAnnotation(annotation);
            copy.getMethodInfo().addAttribute(attr);
            targetClass.addMethod(copy);
            LOGGER.debug("Copied method {}.{} to {}", method.getDeclaringClass().getSimpleName(), method.getName(), targetClass.getSimpleName());
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
                method.getAnnotation(HikariInject.class) != null)                // ignore methods we've injected, they already try..catch
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
                    method.addCatch("throw checkException($e);", exception);
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
                method.getAnnotation(HikariInject.class) != null)                // ignore methods we've injected, they already try..catch
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

    /**
     * 
     */
    static void unregisterInstrumenation()
    {
        ourInstrumentation.removeTransformer(transformer);
    }
}
