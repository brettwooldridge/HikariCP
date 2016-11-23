package com.zaxxer.hikari;

/**
 * Enables configuration of data source in initialization process.
 *
 * @author iobestar
 */
public interface HikariDataSourceConfigurable
{

    void configure(HikariConfig config);

}
