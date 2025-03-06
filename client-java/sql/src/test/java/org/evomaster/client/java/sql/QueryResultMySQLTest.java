package org.evomaster.client.java.sql;

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.GenericContainer;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class QueryResultMySQLTest extends QueryResultTestBase {

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

    @Test
    public void testYearColumn() throws Exception {
        // setup the database
        SqlScriptRunner.execCommand(getConnection(), "CREATE TABLE example_table (\n" +
                "    id INT PRIMARY KEY AUTO_INCREMENT,\n" +
                "    year_column YEAR NOT NULL\n" +
                ");");

        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO example_table (year_column)\n" +
                "VALUES (2022);\n");

        // create the queryResult
        QueryResult queryResult = SqlScriptRunner.execCommand(getConnection(), "SELECT * FROM example_table");
        assertEquals(1, queryResult.seeRows().size());
        DataRow row = queryResult.seeRows().get(0);
        Object actual = row.getValueByName("year_column", "example_table");

        // check the results
        assertTrue(actual instanceof java.util.Date);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date expected = sdf.parse("2022-01-01");
        assertEquals(expected, actual);
    }

}
