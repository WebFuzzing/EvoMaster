package org.evomaster.client.java.controller.internal.db.h2;

import org.evomaster.client.java.controller.internal.SutController;
import org.evomaster.client.java.controller.internal.db.SchemaAnalysistExtractorTest;

import java.sql.Connection;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

public class H2SchemaAnalysistExtractorTest extends DatabaseH2TestInit implements SchemaAnalysistExtractorTest {

    @Override
    public Connection getConnection() {
        return connection;
    }

    @Override
    public SutController getSutController() {
        return new DatabaseFakeH2SutController(connection);
    }
}