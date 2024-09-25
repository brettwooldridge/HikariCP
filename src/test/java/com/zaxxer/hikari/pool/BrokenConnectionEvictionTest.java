/*
 * Copyright (C) 2019 Tim Ward
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

package com.zaxxer.hikari.pool;

import static com.zaxxer.hikari.pool.TestElf.newHikariConfig;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.h2.Driver;
import org.h2.engine.Database;
import org.h2.engine.Session;
import org.h2.jdbc.JdbcConnection;
import org.h2.tools.Server;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * This test class is used to validate that Hikari recovers properly from the
 * situation where the remote database forcibly closes connections, for example
 * as a result of reaching a connection lifetime, while Hikari still holds the
 * connections in its pool.
 * 
 * @author timothyjward
 *
 */
public class BrokenConnectionEvictionTest {

    private static final String COUNT_SESSIONS = "Select COUNT(*) from INFORMATION_SCHEMA.SESSIONS";
    
    private static final String GET_SESSION_IDS = "Select ID from INFORMATION_SCHEMA.SESSIONS WHERE ID <> SESSION_ID()";

    private static final int CONNECTIONS = 3;
    
    private static final String MEMORY_JDBC_URL = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1";

    private static final String NET_JDBC_URL = "jdbc:h2:%s/mem:test";
    

    private Connection con;
    private Server server;

    private HikariDataSource ds;

    @Before
    public void setUp() throws SQLException {
        
        con = DriverManager.getConnection(MEMORY_JDBC_URL);
        
        server = Server.createTcpServer().start();
        
        // Set up a pool for H2 with three connections
        HikariConfig config = newHikariConfig();
        config.setMaximumPoolSize(CONNECTIONS);
        config.setMinimumIdle(CONNECTIONS);
        config.setDriverClassName(Driver.class.getName());
        config.setJdbcUrl(String.format(NET_JDBC_URL, server.getURL()));

        ds = new HikariDataSource(config);
    }

    @After
    public void tearDown() throws SQLException {
        ds.close();
        
        server.shutdown();
        con.close();
    }
    
    @Test
    public void testRemoteCloseLocalInMemory() throws Exception {
        
        // Spin up some threads to execute some queries to ensure all connections in the pool
        // are checked for liveness
        assertTrue("Some threads failed", consumeTheThreadpool().stream().allMatch(e -> e.succeeded));
            
        
        // Use the "unmanaged" connection to kick out all the networked H2 sessions
        List<Integer> ids = getSessionIds(con);
        
        Session session = (Session) ((JdbcConnection) con).getSession();
        Database db = session.getDatabase();
        
        Session[] sessions = db.getSessions(false);
        
        Arrays.stream(sessions)
            .filter(s -> ids.contains(s.getId()))
            .forEach(Session::close);

        // At this point all the connections in the pool are invalid
        // but using the pool should trigger re-creation and things 
        // start working again, except it doesn't...
            
        assertTrue("Some threads failed", consumeTheThreadpool().stream().allMatch(e -> e.succeeded));
    }
    
    private List<ExecuteQuery> consumeTheThreadpool() {
        List<ExecuteQuery> threads = Stream.generate(ExecuteQuery::new)
                    .limit(CONNECTIONS * 2)
                    .collect(toList());
        
        threads.stream().forEach(Thread::start);
        
        System.out.println("testConnectionTestQuery() - Waiting while queries are run");
        threads.stream().forEach(t -> { 
                try {
                    t.join(5000);
                } catch (InterruptedException ie) {}
                assertFalse("The query did not complete in time", t.isAlive());
            });
        return threads;
    }    
    
    private List<Integer> getSessionIds(Connection con) throws SQLException {
        try (Statement statement = con.createStatement()) {
            ResultSet rs = statement.executeQuery(GET_SESSION_IDS);
    
            List<Integer> list = new ArrayList<>();
            
            while(rs.next()) {
                list.add(rs.getInt(1));
            }
            
            return list;
        }
    }

    public class ExecuteQuery extends Thread {
        
        public volatile boolean succeeded = false; 
        
        public void run() {        
            
            try (Connection connection = ds.getConnection()) {
                
                System.out.println("   ExecuteQuery - query to get number of active connections after Db server has restarted");                
                try (Statement statement = connection.createStatement()) {
                    
                    ResultSet rs = statement.executeQuery(COUNT_SESSIONS);
                    
                    if(rs.next()) {
                        
                        Integer numberOfConnections = rs.getInt(1);
                        System.out.println("   ExecuteQuery - numberOfConnections after starting Db server =<" + numberOfConnections + ">");
                        
                    }
                }
                // A short sleep to ensure contention for connections
                Thread.sleep(100);
    
                succeeded = true;
    
            } catch (Exception e) {
                e.printStackTrace();
                succeeded = false;
            }                
        }        
    }
    
}
