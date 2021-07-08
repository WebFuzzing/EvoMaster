package org.evomaster.client.java.controller.internal.db.mysql;

import org.evomaster.client.java.controller.internal.SutController;
import org.evomaster.client.java.controller.internal.db.SqlHandlerInDBTest;

import java.sql.Connection;

public class SqlHandlerInMySQLDBTest extends DatabaseMySQLTestInit implements SqlHandlerInDBTest {
    @Override
    public Connection getConnection() {
        return connection;
    }

    @Override
    public SutController getSutController() {
        return new DatabaseFakeMySQLSutController(connection);
    }
}
