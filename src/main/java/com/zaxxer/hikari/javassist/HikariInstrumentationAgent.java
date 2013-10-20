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

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javassist.bytecode.ClassFile;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import com.zaxxer.hikari.util.ClassLoaderUtils;

/**
 *
 * @author Brett Wooldridge
 */
public class HikariInstrumentationAgent
{
    private static final Logger LOGGER = LoggerFactory.getLogger(HikariInstrumentationAgent.class);

    private static final HashMap<String, Boolean> completionMap;

    // Static initializer
    static
    {
        completionMap = new HashMap<String, Boolean>();
        completionMap.put("java.sql.Connection", false);
        completionMap.put("java.sql.ResultSet", false);
        completionMap.put("java.sql.Statement", false);
        completionMap.put("java.sql.CallableStatement", false);
        completionMap.put("java.sql.PreparedStatement", false);
    }

    private DataSource dataSource;
    private String sniffPackage;

    public HikariInstrumentationAgent(DataSource dataSource)
    {
        this.dataSource = dataSource;
        this.sniffPackage = getDataSourcePackage(dataSource);
    }

    public boolean loadTransformerAgent()
    {
        String jarPath = getSelfJarPath();
        if (jarPath == null)
        {
            LOGGER.warn("Cannot find the HikariCP jar file through introspection.");
            return false;
        }

        try
        {
            registerInstrumentation(jarPath);
            LOGGER.info("Successfully loaded instrumentation agent.  Scanning classes...");
        }
        catch (Exception e)
        {
            LOGGER.warn("Instrumentation agent could not be loaded.  Please report at http://github.com/brettwooldridge/HikariCP.", e);
            return false;
        }

        try
        {
            boolean success = searchInstrumentable(dataSource);
            completionMap.entrySet();
            if (!success)
            {
                LOGGER.warn("Unable to find and instrument necessary classes.  Please report at http://github.com/brettwooldridge/HikariCP.");
                LOGGER.info("Using delegation instead of instrumentation");
            }
            else
            {
                LOGGER.info("Successfully instrumented required JDBC classes.");
            }

            return success;
        }
        catch (Exception e)
        {
            return false;
        }
        finally
        {
            unregisterInstrumenation();
        }
    }

    /**
     * Search the jar (or directory hierarchy) that contains the DataSource and force the class
     * loading of the classes we are about instrumenting.  See loadIfInstrumentable() for more
     * detail.
     *
     * @param dataSource
     * @throws IOException 
     */
    private boolean searchInstrumentable(DataSource dataSource) throws Exception
    {
        String searchPath = getSearchPath(dataSource);
        if (searchPath == null)
        {
            return false;
        }

        long start = System.currentTimeMillis();
        try
        {
            if (searchPath.endsWith(".jar"))
            {
                return searchInstrumentableJar(searchPath);
            }
            else
            {
                String dsSubPath = dataSource.getClass().getPackage().getName().replace('.', '/');
                String classRoot = searchPath.replace(dsSubPath, "");
                // Drop one segment off of the path for a slightly broader search
                searchPath = searchPath.substring(0, searchPath.lastIndexOf('/'));
                return seachInstrumentableDirectory(classRoot, searchPath);
            }
        }
        finally
        {
            LOGGER.info("Instrumentation completed in {}ms.", System.currentTimeMillis() - start);
        }
    }

    private boolean searchInstrumentableJar(String searchPath) throws IOException, ClassNotFoundException
    {
        File jarPath = new File(URI.create(searchPath));
        if (!jarPath.isFile())
        {
            return false;
        }

        JarFile jarFile = new JarFile(jarPath, false, JarFile.OPEN_READ);
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements())
        {
            JarEntry jarEntry = entries.nextElement();
            if (jarEntry.isDirectory())
            {
                continue;
            }

            String entryName = jarEntry.getName();
            if (entryName.endsWith(".class") && entryName.startsWith(sniffPackage) && entryName.indexOf('$') == -1)
            {
                String className = entryName.replace(".class", "").replace('/', '.');
                InputStream inputStream = jarFile.getInputStream(jarEntry);
                loadIfInstrumentable(className, new DataInputStream(inputStream));
                inputStream.close();
            }
        }

        jarFile.close();

        return true;
    }

    /**
     * @param classRoot
     * @param searchPath 
     * @return true if the search completed without error
     * @throws IOException 
     * @throws ClassNotFoundException 
     */
    private boolean seachInstrumentableDirectory(String classRoot, String searchPath) throws IOException, ClassNotFoundException
    {
        File directory = new File(searchPath);
        if (!directory.isDirectory())
        {
            return false;
        }

        for (File fileEntry : directory.listFiles())
        {
            if (fileEntry.isDirectory())
            {
                seachInstrumentableDirectory(classRoot, fileEntry.getPath());
                continue;
            }

            String fileName = fileEntry.getPath();
            String className = fileName.replace(classRoot, "");
            if (className.endsWith(".class") && className.startsWith(sniffPackage) && className.indexOf('$') == -1)
            {
                className = className.replace(".class", "").replace('/', '.');
                InputStream inputStream = new FileInputStream(fileEntry);
                loadIfInstrumentable(className, new DataInputStream(inputStream));
                inputStream.close();
            }
        }

        return true;
    }

    /**
     * If the specified class implements one of the java.sql interfaces we are interested in 
     * instrumenting, use the class loader to cause the class to be loaded.  This will force
     * the class in question through the HikariClassTransformer.
     *
     * @param className the name of the class that might be instrumentable
     * @param classInputStream the stream of bytes for the class file
     * @throws IOException thrown if there is an error reading the class file
     * @throws ClassNotFoundException thrown if the referenced class is not loadable
     */
    private void loadIfInstrumentable(String className, DataInputStream classInputStream) throws IOException, ClassNotFoundException
    {
        ClassFile classFile = new ClassFile(classInputStream);
        if (classFile.isAbstract())
        {
            return;
        }

        for (String iface : classFile.getInterfaces())
        {
            if (!iface.startsWith("java.sql"))
            {
                continue;
            }

            if (completionMap.containsKey(iface))
            {
                LOGGER.info("Instrumenting class {}", className);
                ClassLoaderUtils.loadClass(className);
                completionMap.put(iface, true);
            }
        }
    }

    /**
     * Get the path to the JAR or file system directory where the class of the user
     * specified DataSource implementation resides.
     *
     * @param dataSource the user specified DataSource
     * @return the path to the JAR (including the .jar file name) or a file system classes directory
     */
    private String getSearchPath(DataSource dataSource)
    {
        URL resource = dataSource.getClass().getResource('/' + dataSource.getClass().getName().replace('.', '/') + ".class");
        if (resource == null)
        {
            return null;
        }

        String path = resource.toString();
        if (path.startsWith("jar:"))
        {
            // original form jar:file:/path, make a path like file:///path
            path = path.substring(4, path.indexOf('!')).replace(":/", ":///");
        }
        else if (path.startsWith("file:"))
        {
            path = path.substring(0, path.lastIndexOf('/')).replace("file:", "");
        }
        else
        {
            LOGGER.warn("Could not determine path type of {}", path);
            return null;
        }

        return path;
    }

    /**
     * Given a DataSource class, find the package name that is one-level above the package of
     * the DataSource.  For example, org.hsqldb.jdbc.DataSource -> org.hsqldb.  This is used
     * to filter out packages quickly that we are not interested in instrumenting.
     *
     * @param dataSource a DataSource
     * @return the shortened package name used for filtering
     */
    private String getDataSourcePackage(DataSource dataSource)
    {
        String packageName = dataSource.getClass().getPackage().getName();

        // Count how many segments in the package name.  For example, org.hsqldb.jdbc has three segments.
        int dots = 0;
        int[] offset = new int[16];
        for (int ndx = packageName.indexOf('.'); ndx != -1; ndx = packageName.indexOf('.', ndx + 1))
        {
            offset[dots] = ndx;
            dots++;
        }

        if (dots > 1)
        {
            packageName = packageName.substring(0, offset[dots - 1]);
        }

        return packageName.replace('.', '/');
    }

    /**
     * Get the path to the JAR file from which this class was loaded.
     *
     * @return the path to the jar file that contains this class
     */
    private String getSelfJarPath()
    {
        URL resource = HikariClassTransformer.class.getResource('/' + HikariClassTransformer.class.getName().replace('.', '/') + ".class");
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
     * @throws AttachNotSupportedException thrown if the JVM does not support attachment
     * @throws IOException thrown if the instrumentation JAR cannot be read
     * @throws AgentLoadException thrown if the instrumentation jar does not have proper headers
     * @throws AgentInitializationException thrown if the agent had an error during initialization
     */
    private void registerInstrumentation(String jarPath) throws AttachNotSupportedException, IOException, AgentLoadException, AgentInitializationException
    {
        VirtualMachine vm = VirtualMachine.attach(getPid());
        vm.loadAgent(jarPath, sniffPackage);
        vm.detach();
    }

    /**
     * Unregister the instrumentation (class transformer).
     */
    private void unregisterInstrumenation()
    {
        HikariClassTransformer.unregisterInstrumenation();
    }

    /**
     * Get the PID of the running JVM.
     *
     * @return the process ID (PID) of the running JVM
     */
    private String getPid()
    {
        String nameOfRunningVM = ManagementFactory.getRuntimeMXBean().getName();
        int p = nameOfRunningVM.indexOf('@');
        return nameOfRunningVM.substring(0, p);
    }
}
