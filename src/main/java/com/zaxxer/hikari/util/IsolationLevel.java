/*
 * Copyright (C) 2019 Brett Wooldridge
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

public enum IsolationLevel {
   TRANSACTION_NONE(0, "TRANSACTION_NONE"),
   TRANSACTION_READ_UNCOMMITTED(1, "TRANSACTION_READ_UNCOMMITTED"),
   TRANSACTION_READ_COMMITTED(2, "TRANSACTION_READ_COMMITTED"),
   TRANSACTION_REPEATABLE_READ(4, "TRANSACTION_REPEATABLE_READ"),
   TRANSACTION_SERIALIZABLE(8, "TRANSACTION_SERIALIZABLE"),
   TRANSACTION_SQL_SERVER_SNAPSHOT_ISOLATION_LEVEL(4096,
      "TRANSACTION_SQL_SERVER_SNAPSHOT_ISOLATION_LEVEL");

   private final int levelId;
   private final String levelName;

   IsolationLevel(int levelId, String levelName) {
      this.levelId = levelId;
      this.levelName = levelName;
   }

   public int getLevelId() {
      return levelId;
   }

   public String getLevelName() {
      return levelName;
   }
}
