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
package com.zaxxer.hikari.osgi;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import javax.inject.Inject;

import java.io.File;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.*;

/**
 * @author lburgazzoli
 */
@RunWith(PaxExam.class)
public class OSGiBundleTest
{
    @Inject
    BundleContext context;

    @Configuration
    public Option[] config()
    {
        return options(
            systemProperty("org.osgi.framework.storage.clean").value("true"),
            systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("WARN"),
            mavenBundle("org.slf4j","slf4j-api","1.7.5"),
            mavenBundle("org.slf4j","slf4j-simple","1.7.5").noStart(),
            mavenBundle("org.javassist", "javassist", "3.19.0-GA"),
            new File("target/classes").exists()
                ?  bundle("reference:file:target/classes")
                :  bundle("reference:file:../target/classes"),
            junitBundles(),
            cleanCaches()
        );
    }

    @Test
    public void checkInject()
    {
        assertNotNull(context);
    }

    @Test
    public void checkBundle()
    {
        Boolean bundleFound = false;
        Boolean bundleActive = false;

        Bundle[] bundles = context.getBundles();
        for(Bundle bundle : bundles)
        {
            if(bundle != null)
            {
                if(bundle.getSymbolicName().equals("com.zaxxer.HikariCP"))
                {
                    bundleFound = true;
                    if(bundle.getState() == Bundle.ACTIVE)
                    {
                        bundleActive = true;
                    }
                }
            }
        }

        assertTrue(bundleFound);
        assertTrue(bundleActive);
    }
}
