package org.evomaster.client.java.controller.db;


import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

public class DbCleanerMySQLTest extends DbCleanerTestBase{

    private static final String DB_NAME = "test";

    private static final int PORT = 3306;

    public static GenericContainer mysql = new GenericContainer(new ImageFromDockerfile("mysql-test")
        .withDockerfileFromBuilder(dockerfileBuilder -> {
            dockerfileBuilder.from("mysql:8.0.23")
                    .env("MYSQL_ROOT_PASSWORD", "root_password")
                    .env("MYSQL_DATABASE", "test")
                    .env("MYSQL_USER", "test")
                    .env("MYSQL_PASSWORD", "test");
        }))
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
    }

    @AfterEach
    public void afterTest() throws SQLException {
        // do not find a solution to drop tables without knowing table names, so add a drop method for MySQL
        DbCleaner.dropDatabaseTables_MySQL(connection, DB_NAME, null);
    }


    @Override
    protected Connection getConnection() {
        return connection;
    }

    @Override
    protected void clearDatabase(List<String> tablesToSkip) {
        DbCleaner.clearDatabase(connection, DB_NAME, tablesToSkip, DatabaseType.MYSQL);
    }


    @Override
    protected DatabaseType getDbType() {
        return DatabaseType.MYSQL;
    }
}
