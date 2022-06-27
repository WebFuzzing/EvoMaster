package org.evomaster.client.java.controller.internal.db.h2;

import org.evomaster.client.java.controller.internal.SutController;
import org.evomaster.client.java.controller.db.CollectedSchemasExtractorTestBase;

import java.sql.Connection;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

public class H2CollectedSchemasExtractorTest extends DatabaseH2TestInit implements CollectedSchemasExtractorTestBase {

    @Override
    public Connection getConnection() {
        return connection;
    }

}