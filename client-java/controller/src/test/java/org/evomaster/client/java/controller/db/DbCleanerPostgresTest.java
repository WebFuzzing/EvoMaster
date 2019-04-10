package org.evomaster.client.java.controller.db;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.containers.GenericContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;

/**
 * Created by arcuri82 on 08-Apr-19.
 */
public class DbCleanerPostgresTest extends DbCleanerTestBase{


    private static final GenericContainer postgres = new GenericContainer("postgres:9")
            .withExposedPorts(5432);

    private static Connection connection;

    @BeforeAll
    public static void initClass() throws Exception{
        postgres.start();
        String host = postgres.getContainerIpAddress();
        int port = postgres.getMappedPort(5432);
        String url = "jdbc:postgresql://"+host+":"+port+"/postgres";

        connection = DriverManager.getConnection(url, "postgres", "");
    }

    @AfterAll
    private static void afterClass() throws Exception{
        connection.close();
        postgres.stop();
    }

    @BeforeEach
    public void initTest() throws Exception {
         /*
            see:
            https://stackoverflow.com/questions/3327312/how-can-i-drop-all-the-tables-in-a-postgresql-database
         */
        SqlScriptRunner.execCommand(connection, "DROP SCHEMA public CASCADE;");
        SqlScriptRunner.execCommand(connection, "CREATE SCHEMA public;");
        SqlScriptRunner.execCommand(connection, "GRANT ALL ON SCHEMA public TO postgres;");
        SqlScriptRunner.execCommand(connection, "GRANT ALL ON SCHEMA public TO public;");
    }

    @Override
    protected Connection getConnection(){
        return  connection;
    }

    @Override
    protected void clearDatabase(List<String> tablesToSkip) {
        DbCleaner.clearDatabase_Postgres(connection, "public", tablesToSkip);
    }


}
