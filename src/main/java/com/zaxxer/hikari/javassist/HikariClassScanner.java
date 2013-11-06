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

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Properties;
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

    private Properties codex;

    public HikariClassScanner(HikariClassTransformer transformer)
    {
        instrumentableClasses = new HashMap<>();
        this.transformer = transformer;
    }

    @SuppressWarnings("unchecked")
    public boolean scanClasses(String dsClassName)
    {
        try
        {
            long start = System.currentTimeMillis();

            if (!loadCodex())
            {
                LOGGER.warn("Unable to load instrumentation codex.  Please report at http://github.com/brettwooldridge/HikariCP.");
                LOGGER.info("Using delegation instead of instrumentation");
                return false;
            }

            HashSet<String> hash = (HashSet<String>) codex.get(dsClassName);
            if (hash == null)
            {
                LOGGER.warn("DataSource {} not found in instrumentation codex.  Please report at http://github.com/brettwooldridge/HikariCP.", dsClassName);
                LOGGER.info("Using delegation instead of instrumentation");
                return false;
            }

            String keyPrefix = hash.iterator().next();

            HashSet<String> classes = (HashSet<String>) codex.get(keyPrefix + ".baseConnection");
            loadClasses(classes, HikariClassTransformer.CONNECTION);

            classes = (HashSet<String>) codex.get(keyPrefix + ".subConnection");
            loadClasses(            classes, HikariClassTransformer.CONNECTION_SUBCLASS);

            classes = (HashSet<String>) codex.get(keyPrefix + ".baseStatement");
            loadClasses(classes, HikariClassTransformer.STATEMENT);

            classes = (HashSet<String>) codex.get(keyPrefix + ".subStatement");
            loadClasses(classes, HikariClassTransformer.STATEMENT_SUBCLASS);

            classes = (HashSet<String>) codex.get(keyPrefix + ".basePreparedStatement");
            loadClasses(classes, HikariClassTransformer.PREPARED_STATEMENT);

            classes = (HashSet<String>) codex.get(keyPrefix + ".subPreparedStatement");
            loadClasses(classes, HikariClassTransformer.PREPARED_STATEMENT_SUBCLASS);

            classes = (HashSet<String>) codex.get(keyPrefix + ".baseCallableStatement");
            loadClasses(classes, HikariClassTransformer.CALLABLE_STATEMENT);

            classes = (HashSet<String>) codex.get(keyPrefix + ".subCallableStatement");
            loadClasses(classes, HikariClassTransformer.CALLABLE_STATEMENT_SUBCLASS);

            classes = (HashSet<String>) codex.get(keyPrefix + ".baseResultSet");
            loadClasses(classes, HikariClassTransformer.RESULTSET);

            classes = (HashSet<String>) codex.get(keyPrefix + ".subResultSet");
            loadClasses(classes, HikariClassTransformer.RESULTSET_SUBCLASS);

            LOGGER.info("Instrumented JDBC classes in {}ms.", System.currentTimeMillis() - start);

            return true;
        }
        catch (Exception e)
        {
            return false;
        }
    }

    /**
     * @throws IOException 
     */
    @SuppressWarnings("unchecked")
    private boolean loadCodex() throws IOException
    {
        codex = new Properties();

        InputStream inputStream = this.getClass().getResourceAsStream("/META-INF/codex.properties");
        if (inputStream == null)
        {
            return false;
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        do {
            line = reader.readLine();
            if (line != null)
            {
                line = line.trim();
                if (line.startsWith("#") || line.length() == 0)
                {
                    continue;
                }
    
                String[] split = line.split("=");
                String key = split[0];
                String value = split[1];
                HashSet<String> existing = (HashSet<String>) codex.get(key);
                if (existing == null)
                {
                    HashSet<String> array = new HashSet<>();
                    array.add(value);
                    codex.put(key, array);
                }
                else
                {
                    existing.add(value);
                }
            }
        } while (line != null);

        return true;
    }

    /**
     * @param baseConnections
     * @param connection
     * @throws ClassNotFoundException 
     */
    private void loadClasses(HashSet<String> classes, int classType) throws ClassNotFoundException
    {
        if (classes == null)
        {
            return;
        }

        transformer.setScanClass(classes, classType);
        for (String clazz : classes)
        {
            ClassLoaderUtils.loadClass(clazz);
        }
    }
}
