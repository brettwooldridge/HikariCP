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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Brett Wooldridge
 */
public class HikariClassScanner
{
    private static final Logger LOGGER = LoggerFactory.getLogger(HikariClassScanner.class);

    private HikariClassTransformer transformer;
    
    private Map<String, Set<String>> codex;

    private String shadedCodexMapping;

    public HikariClassScanner(HikariClassTransformer transformer, String shadedCodexMapping)
    {
        this.transformer = transformer;
        this.shadedCodexMapping = shadedCodexMapping;
    }

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

            String keyPrefix = getKeyPrefix(dsClassName);
            if (keyPrefix == null)
            {
                LOGGER.warn("DataSource {} not found in the instrumentation codex.  Please report at http://github.com/brettwooldridge/HikariCP.", dsClassName);
                LOGGER.info("Using delegation instead of instrumentation");
                return false;
            }

            HashSet<String> classes = (HashSet<String>) codex.get(keyPrefix + ".baseConnection");
            loadClasses(classes, HikariClassTransformer.CONNECTION);

            classes = (HashSet<String>) codex.get(keyPrefix + ".subConnection");
            loadClasses(classes, HikariClassTransformer.CONNECTION_SUBCLASS);

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
        	LOGGER.warn("Unable to instrument classes", e);
            return false;
        }
    }

    private String getKeyPrefix(String dsClassName)
    {
        if (shadedCodexMapping == null)
        {
            HashSet<String> hash = (HashSet<String>) codex.get(dsClassName);

            return (hash == null ? null : hash.iterator().next());
        }

        String[] split = shadedCodexMapping.split(":");
        String origPackage = split[0];
        String shadePackage = split[1];

        for (String key : codex.keySet())
        {
            if (key.replace(origPackage, shadePackage).equals(dsClassName))
            {
                HashSet<String> hash = (HashSet<String>) codex.get(key);
                return hash.iterator().next();
            }
        }

        return null;
    }

    /**
     * @throws IOException 
     */
    private boolean loadCodex() throws IOException
    {
        codex = new HashMap<String, Set<String>>();

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

        if (shadedCodexMapping != null)
        {
            String[] split = shadedCodexMapping.split(":");
            String origPackage = split[0];
            String shadePackage = split[1];

            HashSet<String> shadedClasses = new HashSet<>();
            for (String clazz : classes)
            {
                shadedClasses.add(clazz.replace(origPackage, shadePackage));
            }

            classes = shadedClasses;
        }

        transformer.setScanClass(classes, classType);
        for (String clazz : classes)
        {
            this.getClass().getClassLoader().loadClass(clazz);
        }
    }
}
