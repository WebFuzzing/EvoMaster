package org.evomaster.client.java.controller.internal.db.sql.mysql;

import org.evomaster.client.java.controller.internal.SutController;
import org.evomaster.client.java.controller.internal.db.sql.SqlHandlerInDBTest;

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
