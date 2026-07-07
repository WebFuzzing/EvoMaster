package org.evomaster.client.java.sql;

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.containers.GenericContainer;

import java.sql.Connection;
import java.sql.DriverManager;

public class DbInfoExtractorPostgresTest extends DbInfoExtractorTestBase {

    private static final GenericContainer<?> postgres = PostgresContainerUtils.newContainer();

    private static Connection connection;

    @BeforeAll
    public static void initClass() throws Exception{
        postgres.start();
        final String jdbcUrl = PostgresContainerUtils.getJdbcUrl(postgres);
        connection = DriverManager.getConnection(jdbcUrl, "postgres", "");
    }

    @AfterAll
    public static void afterClass() throws Exception{
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
    protected DatabaseType getDbType() {
        return DatabaseType.POSTGRES;
    }


}
