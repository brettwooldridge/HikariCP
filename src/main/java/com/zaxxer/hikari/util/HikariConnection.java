package com.zaxxer.hikari.util;

public enum HikariConnection {

   TRANSACTION_NONE(0),
   TRANSACTION_READ_UNCOMMITTED(1),
   TRANSACTION_READ_COMMITTED(2),
   TRANSACTION_REPEATABLE_READ(4),
   TRANSACTION_SERIALIZABLE(8),
   TRANSACTION_SQL_SERVER_SNAPSHOT_ISOLATION_LEVEL(4096);

   private final int isolationLevel;

   HikariConnection(int isolationLevel) {
      this.isolationLevel = isolationLevel;
   }

   public int getIsolationLevel() {
      return isolationLevel;
   }
}
