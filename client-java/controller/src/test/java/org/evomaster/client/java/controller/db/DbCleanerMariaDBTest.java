package org.evomaster.client.java.controller.db;

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;

public class DbCleanerMariaDBTest extends DbCleanerTestBase{

    private static final String DB_NAME = "test";

    //there exist some problems to start mariadb with GenericContainer, then use MariaDBContainer
    public static MariaDBContainer mariadb = new MariaDBContainer(DockerImageName.parse("mariadb:10.5.9"))
            .withDatabaseName(DB_NAME)
            .withUsername("test")
            .withPassword("test");
    private static Connection connection;

    @BeforeAll
    public static void initClass() throws Exception{
        mariadb.start();
        connection = DriverManager.getConnection(mariadb.getJdbcUrl(), "test","test");
    }

    @AfterAll
    public static void afterClass() throws Exception {
        connection.close();
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
    protected void clearDatabase(List<String> tablesToSkip) {
        DbCleaner.clearDatabase(connection, DB_NAME, tablesToSkip, getDbType());
    }

    @Override
    protected DatabaseType getDbType() {
        return DatabaseType.MARIADB;
    }
}
