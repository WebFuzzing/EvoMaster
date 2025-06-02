package org.evomaster.client.java.sql.h2;

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType;
import org.evomaster.client.java.sql.SqlScriptRunner;
import org.evomaster.client.java.sql.heuristic.SqlHandlerGetDistanceTestBase;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SqlHandlerGetDistanceH2Test extends SqlHandlerGetDistanceTestBase {

    private static Connection connection;

    @Override
    protected Connection getConnection() {
        return connection;
    }

    @Override
    protected DatabaseType getDbType() {
        return DatabaseType.H2;
    }

    @BeforeAll
    public static void initClass() throws Exception {
        connection = DriverManager.getConnection("jdbc:h2:mem:db_test", "sa", "");
    }

    @AfterAll
    public static void afterClass() throws Exception {
        connection.close();
    }


    @Override
    protected void clearDatabase() throws SQLException {
        //custom H2 command
        SqlScriptRunner.execCommand(getConnection(), "DROP ALL OBJECTS;");
    }



}
