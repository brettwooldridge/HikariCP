package com.zaxxer.hikari.pool;

import java.sql.SQLTimeoutException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ConnectionEvictionConfig {
   public static ConnectionEvictionConfig DEFAULT;

   static {
      Set<Class> exceptions = new HashSet<>();
      exceptions.add(SQLTimeoutException.class);

      Set<String> prefixes = new HashSet<>();
      prefixes.add("08"); // BROKEN CONNECTION

      Set<String> states = new HashSet<>();
      states.add("0A000"); // FEATURE UNSUPPORTED
      states.add("57P01"); // ADMIN SHUTDOWN
      states.add("57P02"); // CRASH SHUTDOWN
      states.add("57P03"); // CANNOT CONNECT NOW
      states.add("01002"); // SQL92 disconnect error
      states.add("JZ0C0"); // Sybase disconnect error
      states.add("JZ0C1"); // Sybase disconnect error

      Set<Integer> codes = new HashSet<>();
      codes.add(500150);
      codes.add(2399);

      DEFAULT = new ConnectionEvictionConfig(Collections.unmodifiableSet(exceptions),
         Collections.unmodifiableSet(prefixes), Collections.unmodifiableSet(states), Collections.unmodifiableSet(codes));
   }

   private final Set<Class> terminalErrorExceptions;
   private final Set<String> terminalErrorStatePrefixes;
   private final Set<String> terminalErrorStates;
   private final Set<Integer> terminalErrorCodes;

   public ConnectionEvictionConfig(Set<Class> terminalErrorExceptions, Set<String> terminalErrorStatePrefixes,
                                   Set<String> terminalErrorStates, Set<Integer> terminalErrorCodes) {
      this.terminalErrorExceptions = terminalErrorExceptions != null ? terminalErrorExceptions : DEFAULT.terminalErrorExceptions;
      this.terminalErrorStatePrefixes = terminalErrorStatePrefixes != null ? terminalErrorStatePrefixes : DEFAULT.terminalErrorStatePrefixes;
      this.terminalErrorStates = terminalErrorStates != null ? terminalErrorStates : DEFAULT.terminalErrorStates;
      this.terminalErrorCodes = terminalErrorCodes != null ? terminalErrorCodes : DEFAULT.terminalErrorCodes;
   }

   public Set<Class> getTerminalErrorExceptions() {
      return terminalErrorExceptions;
   }

   public Set<String> getTerminalErrorStatePrefixes() {
      return terminalErrorStatePrefixes;
   }

   public Set<String> getTerminalErrorStates() {
      return terminalErrorStates;
   }

   public Set<Integer> getTerminalErrorCodes() {
      return terminalErrorCodes;
   }
}
