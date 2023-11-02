package org.evomaster.client.java.controller.internal.db.sql.mysql;

import io.restassured.RestAssured;
import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType;
import org.evomaster.client.java.sql.DbCleaner;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.containers.GenericContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.HashMap;

public abstract class DatabaseMySQLTestInit {

    protected static Connection connection;

    protected static final String DB_NAME = "test";

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

    @BeforeAll
    public static void initClass() throws Exception {

        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        mysql.start();

        String host = mysql.getContainerIpAddress();
        int port = mysql.getMappedPort(PORT);
        String url = "jdbc:mysql://"+host+":"+port+"/"+DB_NAME;

        connection = DriverManager.getConnection(url, "test", "test");
    }

    @BeforeEach
    public void initTest() throws Exception {
    }

    @AfterEach
    public void dropTable() {
        DbCleaner.dropDatabaseTables(connection, DB_NAME, null, DatabaseType.MYSQL);
    }

    @AfterAll
    public static void afterClass() throws Exception {
        connection.close();
        mysql.stop();
    }


}
