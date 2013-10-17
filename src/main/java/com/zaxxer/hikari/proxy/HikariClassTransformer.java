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
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.nio.ByteBuffer;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.HashSet;

import javassist.bytecode.ClassFile;

import com.sun.tools.attach.VirtualMachine;

/**
 *
 * @author Brett Wooldridge
 */
public class HikariClassTransformer implements ClassFileTransformer
{
    private String sniffPackage;

    public static void loadTransformerAgent(String sniffPackage)
    {
        String jarPath = getSelfJarPath();
        if (jarPath == null)
        {
            return;
        }

        String nameOfRunningVM = ManagementFactory.getRuntimeMXBean().getName();
        int p = nameOfRunningVM.indexOf('@');
        String pid = nameOfRunningVM.substring(0, p);

        try
        {
            VirtualMachine vm = VirtualMachine.attach(pid);
            vm.loadAgent("/Users/brettw/Documents/dev/HikariCP/target/HikariCP-0.9-SNAPSHOT.jar", sniffPackage);
            vm.detach();
        }
        catch (Exception e)
        {
            return;
        }
    }

    public static void agentmain(String agentArgs, Instrumentation inst)
    {
        inst.addTransformer(new HikariClassTransformer(agentArgs), false);
    }

    HikariClassTransformer(String sniffPackage)
    {
        this.sniffPackage = sniffPackage.replace('.', '/');
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
            String[] interfaces = classFile.getInterfaces();
            return classfileBuffer;
        }
        catch (Exception e)
        {
            return classfileBuffer;
        }
    }

    /**
     * High-speed class file sniffer to determine if the class in question
     * implements the specified interface.
     *
     * @param classFileBytes
     * @return true if the 
     */
    private boolean sniffClass(byte[] classFileBytes)
    {
        ByteBuffer buffer = ByteBuffer.wrap(classFileBytes);

        buffer.getInt();   // 0xCAFEBABE
        buffer.getInt();   // minor/major version

        HashMap<Integer, String> stringPool = new HashMap<Integer, String>(128);
        HashMap<Integer, Integer> classRefs = new HashMap<Integer, Integer>();
        HashMap<Integer, Integer> nameAndDescriptor = new HashMap<Integer, Integer>();
        HashSet<Integer> interfaceRefs = new HashSet<Integer>(); 

        int constantPoolSize = buffer.getShort();
        int slot = 1;
        while (slot < constantPoolSize)
        {
            byte tag = buffer.get();
            switch (tag)
            {
            case 1: // UTF-8 String
                short len = buffer.getShort();
                byte[] buf = new byte[len];
                buffer.get(buf);
                stringPool.put(slot, new String(buf));
                slot++;
                break;
            case 3: // Integer: a signed 32-bit two's complement number in big-endian format
                buffer.getInt();
                slot++;
                break;
            case 4: // Float: a 32-bit single-precision IEEE 754 floating-point number
                buffer.getFloat();
                slot++;
                break;
            case 5: // Long: a signed 64-bit two's complement number in big-endian format (takes two slots in the constant pool table)
                buffer.getLong();
                slot += 2;
                break;
            case 6: // Double: a 64-bit double-precision IEEE 754 floating-point number (takes two slots in the constant pool table)
                buffer.getDouble();
                slot += 2;
                break;
            case 7: // Class reference: an index within the constant pool to a UTF-8 string containing the fully qualified class name
                int index = buffer.getShort();
                classRefs.put(slot, index);
                slot++;
                break;
            case 8: // String reference: an index within the constant pool to a UTF-8 string (big-endian too)
                int sRef = buffer.getShort();
                slot++;
                break;
            case 9: // Field reference: two indexes within the constant pool, the first a Class reference, the second a Name and Type descriptor.
                int fRef1 = buffer.getShort();
                int fRef2 = buffer.getShort();
                slot++;
                break;
            case 10: // Method reference: two indexes within the constant pool, ... 
                int mRef1 = buffer.getShort();
                int mRef2 = buffer.getShort();
                slot++;
                break;
            case 11: // Interface method reference: two indexes within the constant pool, ...
                int iRef1 = buffer.getShort();
                int iRef2 = buffer.getShort();
                interfaceRefs.add(iRef1);
                slot++;
                break;
            case 12: // Name and type descriptor: two indexes to UTF-8 strings within the constant pool, the first representing a name 
                     // (identifier) and the second a specially encoded type descriptor.
                int nameIndex = buffer.getShort();
                int descIndex = buffer.getShort();
                nameAndDescriptor.put(slot, nameIndex);
                slot++;
                break;
            }
        }

        return false;
    }

    private static String getSelfJarPath()
    {
        URL resource = HikariClassTransformer.class.getResource('/' + HikariClassTransformer.class.getName().replace('.', '/') + ".class");
        if (resource == null)
        {
            return null;
        }

        System.out.println(resource);
        String jarPath = resource.toString();
        jarPath = jarPath.replace("file:", "");
        jarPath = jarPath.replace("jar:", "");
        if (jarPath.indexOf('!') > 0)
        {
            jarPath = jarPath.substring(0, jarPath.indexOf('!'));
        }
        else
        {
            jarPath = jarPath.substring(0, jarPath.lastIndexOf('/'));
        }

        return jarPath;
    }
}
