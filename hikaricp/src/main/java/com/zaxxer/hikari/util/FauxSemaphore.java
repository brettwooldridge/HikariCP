/*
 * Copyright (C) 2013, 2014 Brett Wooldridge
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

package com.zaxxer.hikari.util;

import java.util.concurrent.Semaphore;

/**
 * @author Brett Wooldridge
 */
public final class FauxSemaphore extends Semaphore
{
   private static final long serialVersionUID = 7994006542758337519L;

   public static final FauxSemaphore FAUX_SEMAPHORE = new FauxSemaphore();

   /**
    * Default constructor
    */
   public FauxSemaphore()
   {
      super(1);
   }

   /** {@inheritDoc} */
   @Override
   public void acquireUninterruptibly()
   {
   }

   /** {@inheritDoc} */
   @Override
   public void acquireUninterruptibly(int permits)
   {
   }

   /** {@inheritDoc} */
   @Override
   public void release(int permits)
   {
   }

   /** {@inheritDoc} */
   @Override
   public void release()
   {
   }
}
