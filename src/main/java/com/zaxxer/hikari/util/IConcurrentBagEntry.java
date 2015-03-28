/*
 * Copyright (C) 2014 Brett Wooldridge
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

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Brett Wooldridge
 */
public interface IConcurrentBagEntry
{
   int STATE_NOT_IN_USE = 0;
   int STATE_IN_USE = 1;
   int STATE_REMOVED = -1;
   int STATE_RESERVED = -2;

   AtomicInteger state();
}
