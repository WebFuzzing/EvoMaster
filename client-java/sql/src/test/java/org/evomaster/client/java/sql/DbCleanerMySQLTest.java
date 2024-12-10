package org.evomaster.client.java.sql;


import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.GenericContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;

public class DbCleanerMySQLTest extends DbCleanerTestBase{

    private static final String DB_NAME = "test";

    private static final int PORT = 3306;

    private static final String MYSQL_VERSION = "8.0.27";

    public static final GenericContainer mysql = new GenericContainer("mysql:" + MYSQL_VERSION)
            .withEnv(new HashMap<String, String>(){{
                put("MYSQL_ROOT_PASSWORD", "root");
                put("MYSQL_DATABASE", DB_NAME);
                put("MYSQL_USER", "test");
                put("MYSQL_PASSWORD", "test");
            }})
            .withExposedPorts(PORT);

    private static Connection connection;

    @BeforeAll
    public static void initClass() throws Exception{

        mysql.start();

        String host = mysql.getContainerIpAddress();
        int port = mysql.getMappedPort(PORT);
        String url = "jdbc:mysql://"+host+":"+port+"/"+DB_NAME;

        connection = DriverManager.getConnection(url, "test", "test");
    }

    @AfterAll
    public static void afterClass() throws Exception {
        connection.close();
        mysql.stop();
    }

    @AfterEach
    public void afterTest() throws SQLException {
        // do not find a solution to drop tables without knowing table names, so add a drop method for MySQL
        DbCleaner.dropDatabaseTables(connection, DB_NAME, null, getDbType());
    }


    @Override
    protected Connection getConnection() {
        return connection;
    }

    @Override
    protected void clearDatabase(List<String> tablesToSkip, List<String> tableToClean) {
        DbCleaner.clearDatabase(connection, DB_NAME, tablesToSkip, tableToClean, DatabaseType.MYSQL);
    }


    @Override
    protected DatabaseType getDbType() {
        return DatabaseType.MYSQL;
    }
}
