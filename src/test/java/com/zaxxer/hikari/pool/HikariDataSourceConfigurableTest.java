package com.zaxxer.hikari.pool;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.mocks.ConfigurableDataSource;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author iobestar
 * @since 23.11.2016. 17:45
 */
public class HikariDataSourceConfigurableTest
{

    @Test
    public void invokeConfigureOnHikariConfigurableDataSource() throws Exception
    {
        HikariConfig config = new HikariConfig();
        config.setDataSourceClassName("com.zaxxer.hikari.mocks.ConfigurableDataSource");

        try(HikariDataSource ds = new HikariDataSource(config)) {
            Assert.assertTrue(ConfigurableDataSource.configureInvocationCount.get() == 1);
        }
    }

}
