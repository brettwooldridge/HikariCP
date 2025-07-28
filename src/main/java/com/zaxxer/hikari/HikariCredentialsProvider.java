/*
 * Copyright (C) 2025 Brett Wooldridge
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

import com.zaxxer.hikari.util.Credentials;

/**
 * Users can implement this interface to provide credentials for HikariCP.
 * This is useful when credentials need to be dynamically generated or retrieved
 * at runtime, rather than being hardcoded in the configuration.
 */
public interface HikariCredentialsProvider {
      /**
      * This method is called to retrieve the credentials for HikariCP.
      *
      * @return a {@link Credentials} object containing the username and password
      */
   Credentials getCredentials();
}
