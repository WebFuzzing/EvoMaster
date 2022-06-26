package org.evomaster.client.java.controller.internal.db.mysql;

import org.evomaster.client.java.controller.internal.SutController;
import org.evomaster.client.java.controller.internal.db.SchemaAnalysistExtractorTest;
import org.evomaster.client.java.controller.internal.db.SmartDbCleanTest;

import java.sql.Connection;

public class MySQLSchemaAnalysistExtractorTest extends DatabaseMySQLTestInit implements SchemaAnalysistExtractorTest {

    @Override
    public Connection getConnection() {
        return connection;
    }

    @Override
    public SutController getSutController() {
        return new DatabaseFakeMySQLSutController(connection);
    }
}
