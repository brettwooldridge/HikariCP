package com.zaxxer.hikari;

import java.sql.SQLException;
import java.sql.SQLTimeoutException;

/**
 * Users can implement this interface to override the default SQLException eviction handling of HikariCP.
 * By the time an instance of this interface is invoked HikariCP has already made a determination the connection is a
 * candidate to evict from the pool.
 *
 * If the {@link #adjudicate(SQLException)} method returns {@link Override#CONTINUE_EVICT} the eviction will occur, but
 * if the method returns {@link Override#DO_NOT_EVICT} the eviction will be elided.
 *
 * If an implementation isn't provided in the connection pool configuration, HikariCP will install a default
 * instance that does not evict if the exception is derived from #SQLTimeoutException, otherwise the eviction will
 * proceed.
 */
public interface SQLExceptionOverride {
   enum Override {
      CONTINUE_EVICT,
      DO_NOT_EVICT
   }

   /**
    * If this method returns {@link Override#CONTINUE_EVICT} then Connection eviction will occur, but if it
    * returns {@link Override#DO_NOT_EVICT} the eviction will be elided.
    *
    * @param sqlException the #SQLException to adjudicate
    * @return either one of {@link Override#CONTINUE_EVICT} or {@link Override#DO_NOT_EVICT}
    */
   default Override adjudicate(final SQLException sqlException)
   {
      return sqlException instanceof SQLTimeoutException ? Override.DO_NOT_EVICT : Override.CONTINUE_EVICT;
   }
}
