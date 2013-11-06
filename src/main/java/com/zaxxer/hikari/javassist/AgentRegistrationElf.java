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

    public static boolean loadTransformerAgent(String dsClassName)
    {
        String agentJarPath = getSelfJarPath();
        if (agentJarPath == null)
        {
            LOGGER.warn("Cannot find the HikariCP jar file through introspection.");
            return false;
        }

        try
        {

            Properties systemProperties = System.getProperties();
            systemProperties.put("com.zaxxer.hikari.classloader", AgentRegistrationElf.class.getClassLoader());

            HikariClassTransformer transformer = new HikariClassTransformer();
            systemProperties.put("com.zaxxer.hikari.transformer", transformer);

            registerInstrumentation(agentJarPath);
            LOGGER.info("Successfully loaded instrumentation agent.  Scanning classes...");

            HikariClassScanner scanner = new HikariClassScanner(transformer);
            scanner.scanClasses(dsClassName);

            return true;
        }
        catch (Exception e)
        {
            LOGGER.warn("Instrumentation agent could not be loaded.  Please report at http://github.com/brettwooldridge/HikariCP.", e);
            return false;
        }
//        finally
//        {
//            unregisterInstrumenation();
//        }
    }

    /**
     * Get the path to the JAR file from which this class was loaded.
     *
     * @return the path to the jar file that contains this class
     */
    private static String getSelfJarPath()
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
            return System.getProperty("com.zaxxer.hikari.selfJar");
        }

        return jarPath;
    }

    /**
     * Attempt to register our instrumentation (class transformer) with the virtual machine
     * dynamically.
     *
     * @param jarPath the path to our own jar file
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
