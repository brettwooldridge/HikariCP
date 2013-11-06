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
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javassist.bytecode.ClassFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.util.ClassLoaderUtils;

/**
 *
 * @author Brett Wooldridge
 */
public class HikariClassScanner
{
    private static final Logger LOGGER = LoggerFactory.getLogger(HikariClassScanner.class);

    private static final LinkedHashMap<String, Boolean> completionMap;
    private final HashMap<String, ClassFile> instrumentableClasses;

    // Static initializer
    static
    {
        completionMap = new LinkedHashMap<>();
        completionMap.put("java.sql.Connection", false);
        completionMap.put("java.sql.ResultSet", false);
        completionMap.put("java.sql.CallableStatement", false);
        completionMap.put("java.sql.PreparedStatement", false);
        completionMap.put("java.sql.Statement", false);
    }

    private HikariClassTransformer transformer;
    
    private String sniffPackage;

    public HikariClassScanner(HikariClassTransformer transformer)
    {
        instrumentableClasses = new HashMap<>();
        this.transformer = transformer;
    }

    public boolean scanClasses(String dsClassName)
    {
        try
        {
            long start = System.currentTimeMillis();

            sniffPackage = getDataSourceSubPackage(dsClassName);

            boolean couldScan = searchInstrumentable(dsClassName);
            if (!couldScan)
            {
                LOGGER.warn("Unable to find and instrument necessary classes.  Please report at http://github.com/brettwooldridge/HikariCP.");
                LOGGER.info("Using delegation instead of instrumentation");
                return false;
            }

            HashSet<String> interfaces = findInterfaces("java.sql.Connection");
            HashSet<String> rootClasses = findRootClasses(interfaces, HikariClassTransformer.CONNECTION);
            findSubclasses(rootClasses, HikariClassTransformer.CONNECTION_SUBCLASS);

            interfaces = findInterfaces("java.sql.Statement");
            rootClasses = findRootClasses(interfaces, HikariClassTransformer.STATEMENT);
            findSubclasses(rootClasses, HikariClassTransformer.STATEMENT_SUBCLASS);

            interfaces = findInterfaces("java.sql.PreparedStatement");
            rootClasses = findRootClasses(interfaces, HikariClassTransformer.PREPARED_STATEMENT);
            findSubclasses(rootClasses, HikariClassTransformer.PREPARED_STATEMENT_SUBCLASS);

            interfaces = findInterfaces("java.sql.CallableStatement");
            rootClasses = findRootClasses(interfaces, HikariClassTransformer.CALLABLE_STATEMENT);
            findSubclasses(rootClasses, HikariClassTransformer.CALLABLE_STATEMENT_SUBCLASS);

            interfaces = findInterfaces("java.sql.ResultSet");
            rootClasses = findRootClasses(interfaces, HikariClassTransformer.RESULTSET);
            findSubclasses(rootClasses, HikariClassTransformer.RESULTSET_SUBCLASS);

            LOGGER.info("Instrumented JDBC classes in {}ms.", System.currentTimeMillis() - start);

            return true;
        }
        catch (Exception e)
        {
            return false;
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
    private boolean searchInstrumentable(String dsClassName) throws Exception
    {
        String searchPath = getSearchPath(dsClassName);
        if (searchPath == null)
        {
            return false;
        }

        if (searchPath.endsWith(".jar"))
        {
            return scanInstrumentableJar(searchPath);
        }
        else
        {
            String dsSubPath = getDataSourceSubPackage(dsClassName).replace('.', '/');
            String classRoot = searchPath.replace(dsSubPath, "");
            // Drop one segment off of the path for a slightly broader search
            searchPath = searchPath.substring(0, searchPath.lastIndexOf('/'));
            return scanInstrumentableDirectory(classRoot, searchPath);
        }
    }

    private boolean scanInstrumentableJar(String searchPath) throws IOException, ClassNotFoundException
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
                InputStream inputStream = jarFile.getInputStream(jarEntry);
                trackClass(inputStream);
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
    private boolean scanInstrumentableDirectory(String classRoot, String searchPath) throws IOException, ClassNotFoundException
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
                scanInstrumentableDirectory(classRoot, fileEntry.getPath());
                continue;
            }

            String fileName = fileEntry.getPath();
            String className = fileName.replace(classRoot, "");
            if (className.endsWith(".class") && className.startsWith(sniffPackage) && className.indexOf('$') == -1)
            {
                className = className.replace(".class", "").replace('/', '.');
                InputStream inputStream = new FileInputStream(fileEntry);
                trackClass(inputStream);
                inputStream.close();
            }
        }

        return true;
    }

    private void trackClass(InputStream inputStream) throws IOException
    {
        DataInputStream dis = new DataInputStream(inputStream);
        ClassFile classFile = new ClassFile(dis);

        instrumentableClasses.put(classFile.getName(), classFile);
    }

    private HashSet<String> findInterfaces(String interfaceName)
    {
        HashSet<String> subInterfaces = new HashSet<>();
        subInterfaces.add(interfaceName);

        for (ClassFile classFile : instrumentableClasses.values())
        {
            if (!classFile.isInterface())
            {
                continue;
            }

            HashSet<String> interfaces = new HashSet<>(Arrays.asList(classFile.getInterfaces()));
            if (interfaces.contains(interfaceName))
            {
                subInterfaces.add(classFile.getName());
            }
        }

        return subInterfaces;
    }

    private HashSet<String> findRootClasses(HashSet<String> interfaces, int classType) throws ClassNotFoundException
    {
        HashSet<String> rootClasses = new HashSet<>();

        for (ClassFile classFile : instrumentableClasses.values())
        {
            if (classFile.isInterface())
            {
                continue;
            }

            HashSet<String> ifaces = new HashSet<>(Arrays.asList(classFile.getInterfaces()));
            ifaces.retainAll(interfaces);
            if (ifaces.size() == 0)
            {
                continue;
            }

            // Now we found a class that implements java.sql.Connection
            // ... walk up to the top of the hierarchy
            String currentClass = classFile.getName();
            while (true)
            {
                ClassFile superClass = instrumentableClasses.get(currentClass);
                if (superClass == null)
                {
                    break;
                }

                String maybeSuper = superClass.getSuperclass();
                if (maybeSuper.equals("java.lang.Object"))
                {
                    rootClasses.add(superClass.getName());
                    break;
                }
                
                currentClass = maybeSuper; 
            }
        }

        if (rootClasses.isEmpty())
        {
            throw new RuntimeException("Unable to find root class implementation of " + interfaces);
        }

        transformer.setScanClass(rootClasses, classType);
        for (String rootClass : rootClasses)
        {
            ClassLoaderUtils.loadClass(rootClass);
        }

        transformer.setScanClass(new HashSet<String>(), HikariClassTransformer.UNDEFINED);

        return rootClasses;
    }

    private void findSubclasses(HashSet<String> rootClasses, int classType) throws ClassNotFoundException
    {
        HashSet<String> subClasses = new HashSet<>();
        subClasses.addAll(rootClasses);

        boolean exhausted;
        do {
            exhausted = true;
            for (ClassFile classFile : instrumentableClasses.values())
            {
                if (subClasses.contains(classFile.getSuperclass()) && !subClasses.contains(classFile.getName()))
                {
                    exhausted = false;
                    subClasses.add(classFile.getName());
                }
            }
        } while (!exhausted);

        if (subClasses.size() > 1)
        {
            subClasses.removeAll(rootClasses);
        }

        transformer.setScanClass(subClasses, classType);
        for (String subClass : subClasses)
        {
            ClassLoaderUtils.loadClass(subClass);
        }

        subClasses.clear();
        transformer.setScanClass(subClasses, HikariClassTransformer.UNDEFINED);
    }

    /**
     * Get the path to the JAR or file system directory where the class of the user
     * specified DataSource implementation resides.
     *
     * @param dataSource the user specified DataSource
     * @return the path to the JAR (including the .jar file name) or a file system classes directory
     */
    private String getSearchPath(String dsClassName)
    {
        URL resource = this.getClass().getResource('/' + dsClassName.replace('.', '/') + ".class");
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
    static String getDataSourceSubPackage(String dsClassName)
    {
        String packageName = dsClassName.substring(0, dsClassName.lastIndexOf('.'));

        // Count how many segments in the package name.  For example, org.hsqldb.jdbc has three segments.
        int dots = 0;
        int[] offset = new int[16];
        for (int ndx = packageName.indexOf('.'); ndx != -1; ndx = packageName.indexOf('.', ndx + 1))
        {
            offset[dots] = ndx;
            dots++;
        }

        if (dots > 3)
        {
            packageName = packageName.substring(0, offset[dots - 2]);
        }
        else if (dots > 1)
        {
            packageName = packageName.substring(0, offset[dots - 1]);
        }

        return packageName.replace('.', '/');
    }
}
