package com.zaxxer.hikari.util;

import javax.management.ConstructorParameters;

/**
 * A simple class to hold connection credentials and is designed to be immutable.
 */
public final class Credentials
{

   private final String username;
   private final String password;

   /**
    * Construct an immutable Credentials object with the supplied username and password.
    *
    * @param username the username
    * @param password the password
    * @return a new Credentials object
    */
   public static Credentials of(final String username, final String password) {
      return new Credentials(username, password);
   }

   /**
    * Construct an immutable Credentials object with the supplied username and password.
    *
    * @param username the username
    * @param password the password
    */
   @ConstructorParameters({ "username", "password" })
   public Credentials(final String username, final String password)
   {
      this.username = username;
      this.password = password;
   }

   /**
    * Get the username.
    *
    * @return the username
    */
   public String getUsername()
   {
      return username;
   }

   /**
    * Get the password.
    *
    * @return the password
    */
   public String getPassword()
   {
      return password;
   }
}
