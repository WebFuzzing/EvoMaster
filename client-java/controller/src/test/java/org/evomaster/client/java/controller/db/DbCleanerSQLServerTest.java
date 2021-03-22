package org.evomaster.client.java.controller.db;

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;

public class DbCleanerSQLServerTest extends DbCleanerTestBase{

    private static final int PORT = 1433;
    private static final String PASSWORD = "A_Str0ng_Required_Password";

    public static final GenericContainer mssqlserver = new GenericContainer(DockerImageName.parse("mcr.microsoft.com/mssql/server").withTag("2019-CU9-ubuntu-16.04"))
            .withEnv(new HashMap<String, String>(){{
                put("ACCEPT_EULA", "Y");
                put("SA_PASSWORD", PASSWORD);
            }})
            .withStartupTimeout(Duration.ofSeconds(240))
            .withExposedPorts(PORT);

    private static Connection connection;


    //driver name "com.microsoft.sqlserver.jdbc.SQLServerDriver"
    @BeforeAll
    public static void initClass() throws SQLException {
        mssqlserver.start();

        String host = mssqlserver.getContainerIpAddress();
        int port = mssqlserver.getMappedPort(PORT);
        String url = "jdbc:sqlserver://"+host+":"+port;

        connection = DriverManager.getConnection(url, "SA", PASSWORD);
    }

    @AfterAll
    public static void afterClass() throws SQLException {
        connection.close();
    }

    @AfterEach
    public void afterTest() throws SQLException {
        // the drop needs to be executed multiple times if there exist fk
        SqlScriptRunner.execCommand(connection, "EXEC sp_msforeachtable 'drop table ?'");
        SqlScriptRunner.execCommand(connection, "EXEC sp_msforeachtable 'drop table ?'");
    }

    @Override
    protected Connection getConnection() {
        return connection;
    }

    @Override
    protected void clearDatabase(List<String> tablesToSkip) {
        DbCleaner.clearDatabase(connection, tablesToSkip, getDbType());
    }

    @Override
    protected DatabaseType getDbType() {
        return DatabaseType.MS_SQL_SERVER;
    }
}
