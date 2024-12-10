package org.evomaster.client.java.sql;

import org.evomaster.ci.utils.CIUtils;
import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType;
import org.evomaster.client.java.sql.DbCleaner;
import org.evomaster.client.java.sql.QueryResult;
import org.evomaster.client.java.sql.SqlScriptRunner;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.DriverManager;
import java.time.Duration;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

public class DbCleanerSQLServerWithSchemaTest {

    private static final int PORT = 1433;
    private static final String PASSWORD = "A_Str0ng_Required_Password";

    public static final GenericContainer mssqlserver = new GenericContainer(DockerImageName.parse("mcr.microsoft.com/mssql/server").withTag("2019-CU9-ubuntu-16.04"))
            .withEnv(new HashMap<String, String>(){{
                put("ACCEPT_EULA", "Y");
                put("MSSQL_SA_PASSWORD", PASSWORD);
            }})
            .withStartupTimeout(Duration.ofSeconds(240))
            .withExposedPorts(PORT);

    private static Connection connection;

    @Test
    public void testSkipTableMisconfigured() throws Exception{
        /*
         As checked, passed on local
         */

        CIUtils.skipIfOnGA();
        CIUtils.skipIfOnCircleCI();
        CIUtils.skipIfOnWindows();

        mssqlserver.start();

        String host = mssqlserver.getContainerIpAddress();
        int port = mssqlserver.getMappedPort(PORT);
        String url = "jdbc:sqlserver://"+host+":"+port;

        connection = DriverManager.getConnection(url, "sa", PASSWORD);

        assertTrue(connection != null);
        SqlScriptRunner.execCommand(connection, "CREATE SCHEMA Foo AUTHORIZATION dbo;");
        SqlScriptRunner.execCommand(connection, "CREATE TABLE Foo.Foo(x int, primary key (x));");
        SqlScriptRunner.execCommand(connection, "INSERT INTO Foo.Foo(x) VALUES (42)");

        QueryResult res = SqlScriptRunner.execCommand(connection, "SELECT * FROM Foo.Foo;");
        assertEquals(1, res.seeRows().size());


        assertThrows(RuntimeException.class, ()->
                DbCleaner.clearDatabase(connection, "", null, null, DatabaseType.MS_SQL_SERVER)
        );

        DbCleaner.clearDatabase(connection, "Foo", null, null, DatabaseType.MS_SQL_SERVER);
        res = SqlScriptRunner.execCommand(connection, "SELECT * FROM Foo.Foo;");
        assertEquals(0, res.seeRows().size());
    }
}
