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

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;

/**
 *
 * @author Brett Wooldridge
 */
public class AgentRegistrationElf
{
    private static final Logger LOGGER = LoggerFactory.getLogger(HikariClassScanner.class);

    public static boolean loadTransformerAgent(String dsClassName, String shadedCodexMapping)
    {
        String agentJarPath = getAgentJarPath();
        if (agentJarPath == null)
        {
            LOGGER.info("Cannot find the HikariCP-agent jar file through introspection.");
            return false;
        }

        try
        {
            Properties systemProperties = System.getProperties();

            HikariClassTransformer transformer = new HikariClassTransformer();
            systemProperties.put("com.zaxxer.hikari.transformer", transformer);

            registerInstrumentation(agentJarPath);
            LOGGER.info("Successfully loaded instrumentation agent.  Scanning classes...");

            HikariClassScanner scanner = new HikariClassScanner(transformer, shadedCodexMapping);
            return scanner.scanClasses(dsClassName);
        }
        catch (Exception e)
        {
            LOGGER.warn("Instrumentation agent could not be loaded.  Please report at http://github.com/brettwooldridge/HikariCP.", e);
            return false;
        }
        finally
        {
            if (unregisterInstrumenation())
            {
                LOGGER.info("Unloaded instrumentation agent.");
            }
        }
    }

    /**
     * Get the path to the JAR file from which this class was loaded.
     *
     * @return the path to the jar file that contains this class
     */
    private static String getAgentJarPath()
    {
        URL resource = AgentRegistrationElf.class.getResource("/com/zaxxer/hikari/javassist/HikariInstrumentationAgent.class");
        if (resource == null)
        {
            return null;
        }

        String jarPath = resource.toString();
        jarPath = jarPath.replace("file:", "");
        jarPath = jarPath.replace("jar:", "");
        if (jarPath.indexOf('!') > 0)
        {
            jarPath = jarPath.substring(0, jarPath.indexOf('!'));
        }
        else
        {
            return System.getProperty("com.zaxxer.hikari.agentJar");
        }

        return jarPath;
    }

    /**
     * Attempt to register our instrumentation (class transformer) with the virtual machine
     * dynamically.
     *
     * @param jarPath the path to our own jar file
     * @param shadedCodexMapping 
     * @param dsClassName 
     * @throws AttachNotSupportedException thrown if the JVM does not support attachment
     * @throws IOException thrown if the instrumentation JAR cannot be read
     * @throws AgentLoadException thrown if the instrumentation jar does not have proper headers
     * @throws AgentInitializationException thrown if the agent had an error during initialization
     */
    private static void registerInstrumentation(String jarPath) throws AttachNotSupportedException, IOException, AgentLoadException, AgentInitializationException
    {
        VirtualMachine vm = VirtualMachine.attach(getPid());
        vm.loadAgent(jarPath);
        vm.detach();
    }

    private static boolean unregisterInstrumenation()
    {
        boolean unregistered = false;

        Properties systemProperties = System.getProperties();
        Instrumentation instrumentation = (Instrumentation) systemProperties.get("com.zaxxer.hikari.instrumentation");
        if (instrumentation != null)
        {
            ClassFileTransformer transformer = (ClassFileTransformer) systemProperties.get("com.zaxxer.hikari.transformer");
            instrumentation.removeTransformer(transformer);
            unregistered = true;
        }

        systemProperties.remove("com.zaxxer.hikari.instrumentation");
        systemProperties.remove("com.zaxxer.hikari.transformer");

        return unregistered;
    }

    /**
     * Get the PID of the running JVM.
     *
     * @return the process ID (PID) of the running JVM
     */
    private static String getPid()
    {
        String nameOfRunningVM = ManagementFactory.getRuntimeMXBean().getName();
        int p = nameOfRunningVM.indexOf('@');
        return nameOfRunningVM.substring(0, p);
    }
}
