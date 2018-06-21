package com.test;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class Main {
   public static void main(String[] args) {
      new Main().test();
   }

   private void test() {
      try {
         HikariConfig config = new HikariConfig(
            "D:\\project\\javaproject\\HikariCP\\target" + "\\classes\\hikari.properties");
         config.setDriverClassName("com.mysql.cj.jdbc.Driver");
         HikariDataSource ds = new HikariDataSource(config);

         Connection connection = ds.getConnection();
         PreparedStatement preparedStatement =
            connection.prepareStatement("select * from sys_config");
         ResultSet resultSet = preparedStatement.executeQuery();
         while (resultSet.next()) {
            System.out.println(resultSet.getString(1));
         }

         resultSet.close();
         preparedStatement.close();
         connection.close();


      } catch (Exception ex) {
         ex.printStackTrace();
      }

   }

}
