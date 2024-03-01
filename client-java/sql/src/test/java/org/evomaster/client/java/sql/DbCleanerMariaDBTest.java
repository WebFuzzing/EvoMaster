package org.evomaster.client.java.sql;

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.GenericContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.HashMap;
import java.util.List;

public class DbCleanerMariaDBTest extends DbCleanerTestBase{

    private static final String DB_NAME = "test";

    private static final int PORT = 3306;

    public static final GenericContainer mariadb = new GenericContainer("mariadb:10.5.9")
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
        mariadb.start();

        String host = mariadb.getContainerIpAddress();
        int port = mariadb.getMappedPort(PORT);
        String url = "jdbc:mariadb://"+host+":"+port+"/"+DB_NAME;

        connection = DriverManager.getConnection(url, "test", "test");
    }

    @AfterAll
    public static void afterClass() throws Exception {
        connection.close();
        mariadb.stop();
    }

    @AfterEach
    public void afterTest(){
        DbCleaner.dropDatabaseTables(connection, DB_NAME, null, getDbType());
    }

    @Override
    protected Connection getConnection() {
        return connection;
    }

    @Override
    protected void clearDatabase(List<String> tablesToSkip, List<String> tableToClean) {
        DbCleaner.clearDatabase(connection, DB_NAME, tablesToSkip, tableToClean, getDbType());
    }

    @Override
    protected DatabaseType getDbType() {
        return DatabaseType.MARIADB;
    }
}
