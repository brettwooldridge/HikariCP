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

package com.zaxxer.hikari.proxy;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

import javassist.bytecode.ClassFile;

/**
 *
 * @author Brett Wooldridge
 */
public class HikariClassTransformer implements ClassFileTransformer
{
    // private static final Logger LOGGER = LoggerFactory.getLogger(HikariClassTransformer.class);

    private static Instrumentation ourInstrumentation;
    private static HikariClassTransformer transformer;

    private String sniffPackage;

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
            for (String iface : classFile.getInterfaces())
            {
                if (!iface.startsWith("java.sql"))
                {
                    continue;
                }

                if (iface.equals("java.sql.Connection"))
                {
                    transformConnection(classFile);
                }
                else if (iface.equals("java.sql.PreparedStatement"))
                {
                    transformPreparedStatement(classFile);
                }
                else if (iface.equals("java.sql.CallableStatement"))
                {
                    transformCallableStatement(classFile);
                }
                else if (iface.equals("java.sql.Statement"))
                {
                    transformStatement(classFile);
                }
                else if (iface.equals("java.sql.ResultSet"))
                {
                    transformResultSet(classFile);
                }
            }

            // None of the interfaces we care about were found, so just return the class file buffer
            return classfileBuffer;
        }
        catch (Exception e)
        {
            return classfileBuffer;
        }
    }

    /**
     * @param classFile
     */
    private void transformConnection(ClassFile classFile)
    {
    }

    /**
     * @param classFile
     */
    private void transformPreparedStatement(ClassFile classFile)
    {
    }

    /**
     * @param classFile
     */
    private void transformCallableStatement(ClassFile classFile)
    {
    }

    /**
     * @param classFile
     */
    private void transformStatement(ClassFile classFile)
    {
    }

    /**
     * @param classFile
     */
    private void transformResultSet(ClassFile classFile)
    {
    }

    /**
     * 
     */
    static void unregisterInstrumenation()
    {
        ourInstrumentation.removeTransformer(transformer);
    }
}
