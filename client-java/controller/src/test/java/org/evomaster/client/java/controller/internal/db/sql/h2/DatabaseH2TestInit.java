package org.evomaster.client.java.controller.internal.db.sql.h2;

import io.restassured.RestAssured;
import org.evomaster.client.java.sql.SqlScriptRunner;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.sql.Connection;
import java.sql.DriverManager;

public abstract class DatabaseH2TestInit {

    protected static Connection connection;

    @BeforeAll
    public static void initClass() throws Exception {

        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        connection = DriverManager.getConnection("jdbc:h2:mem:db_test", "sa", "");
    }

    @BeforeEach
    public void initTest() throws Exception {

        /*
            Not supported in H2
            SqlScriptRunner.execCommand(connection, "DROP DATABASE db_test;");
            SqlScriptRunner.execCommand(connection, "CREATE DATABASE db_test;");
        */

        //custom H2 command
        SqlScriptRunner.execCommand(connection, "DROP ALL OBJECTS;");
    }

}
