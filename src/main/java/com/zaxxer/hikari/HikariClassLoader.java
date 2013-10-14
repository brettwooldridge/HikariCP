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

package com.zaxxer.hikari;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.Loader;
import javassist.NotFoundException;
import javassist.bytecode.ClassFile;

public class HikariClassLoader extends Loader
{
    private String dataSourceClassName;
    private ClassPool classPool;

    static
    {
        ClassPool.releaseUnmodifiedClassFile = true;
        ClassPool.doPruning = true;
    }

    HikariClassLoader(String dataSourceClassName)
    {
        this.dataSourceClassName = dataSourceClassName;
        ClassPool parentPool = ClassPool.getDefault();
        this.classPool = new ClassPool(parentPool);
        this.classPool.childFirstLookup = true;
    }

    @Override
    protected Class<?> findClass(String className) throws ClassNotFoundException
    {
        CtClass ctClass;
        CtClass[] interfaces;
        try
        {
            ctClass = classPool.get(className);
            interfaces = ctClass.getInterfaces();
        }
        catch (NotFoundException e)
        {
            throw new ClassNotFoundException("HikariClassLoader could not load class " + className, e);
        }

        boolean modified = false;
//        ClassFile classFile = ctClass.getClassFile();
//        String[] interfaces = classFile.getInterfaces();
        for (CtClass interf : interfaces)
        {
            if ("java.sql.Connection".equals(interf.getName()))
            {
                modified = true;
                break;
            }
        }

        if (!modified)
        {
            ctClass.detach();
        }

        return super.findClass(className);
    }
}
