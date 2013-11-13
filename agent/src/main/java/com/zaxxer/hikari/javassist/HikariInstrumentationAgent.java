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

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.util.Properties;

/**
 * This class provides the "agentmain" method of the instrumentation agent.
 *
 * @author Brett Wooldridge
 */
public class HikariInstrumentationAgent
{
    /**
     * The method that is called when VirtualMachine.loadAgent() is invoked to register our
     * class transformer.
     *
     * @param agentArgs arguments to pass to the agent
     * @param instrumentation the virtual machine Instrumentation instance used to register our transformer 
     */
    public static void agentmain(String agentArgs, Instrumentation instrumentation)
    {
        Properties systemProperties = System.getProperties();
        systemProperties.put("com.zaxxer.hikari.instrumentation", instrumentation);

        ClassFileTransformer transformer = (ClassFileTransformer) systemProperties.get("com.zaxxer.hikari.transformer");
        instrumentation.addTransformer(transformer, false);
    }
}
