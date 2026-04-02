package org.evomaster.client.java.sql;

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DbInfoExtractorMySQLTest extends DbInfoExtractorTestBase {

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
    public static void initClass() throws Exception {

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
        SqlScriptRunner.execCommand(connection, "DROP TABLE IF EXISTS example_table;");
    }


    @Override
    protected DatabaseType getDbType() {
        return DatabaseType.MYSQL;
    }

    @Override
    protected Connection getConnection() {
        return connection;
    }


}
