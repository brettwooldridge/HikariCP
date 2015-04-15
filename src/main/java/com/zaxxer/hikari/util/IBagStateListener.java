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

import java.util.concurrent.Future;

/**
 * This interface is implemented by a listener to the ConcurrentBag.  The
 * listener will be informed of when the bag has become empty.  The usual
 * course of action by the listener in this case is to attempt to add an
 * item to the bag.
 *
 * @author Brett Wooldridge
 */
public interface IBagStateListener
{
   Future<Boolean> addBagItem();
}
