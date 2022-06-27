package org.evomaster.client.java.controller.internal.db.postgres;

import io.restassured.RestAssured;
import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType;
import org.evomaster.client.java.controller.db.DbCleaner;
import org.evomaster.client.java.controller.db.SqlScriptRunner;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.containers.GenericContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.HashMap;
import java.util.LinkedHashMap;

public abstract class DatabasePostgresTestInit {

    protected static Connection connection;

    private static final String DB_NAME = "postgres";

    private static final String POSTGRES_VERSION = "14";

    private static final int PORT = 5432;

    private static final GenericContainer<?> postgres = new GenericContainer<>("postgres:" + POSTGRES_VERSION)
            .withExposedPorts(PORT)
            .withEnv("POSTGRES_HOST_AUTH_METHOD","trust");

    @BeforeAll
    public static void startContainer() throws Exception {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        postgres.start();
        String host = postgres.getContainerIpAddress();
        int port = postgres.getMappedPort(PORT);
        String url = "jdbc:postgresql://"+host+":"+port+"/" +DB_NAME;

        connection = DriverManager.getConnection(url, "postgres", "");
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

    @AfterEach
    public void dropTable() {
    }

    @AfterAll
    public static void closeAndStopAll() throws Exception {
        connection.close();
        postgres.stop();
    }


}
