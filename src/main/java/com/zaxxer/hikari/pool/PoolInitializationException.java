/*
 * Copyright (C) 2015 Brett Wooldridge
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

package com.zaxxer.hikari.pool;

/**
 * A custom exception thrown if pool initialization fails.
 *
 * @author Brett Wooldridge
 */
public class PoolInitializationException extends RuntimeException
{
   private static final long serialVersionUID = 929872118275916520L;

   /**
    * Construct an exception, possibly wrapping the provided Throwable as the cause.
    */
   public PoolInitializationException(Throwable t)
   {
      super("Exception during pool initialization", t);
   }
}
