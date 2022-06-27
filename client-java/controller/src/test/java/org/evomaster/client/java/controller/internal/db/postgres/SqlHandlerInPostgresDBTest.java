package org.evomaster.client.java.controller.internal.db.postgres;

import org.evomaster.client.java.controller.internal.SutController;
import org.evomaster.client.java.controller.internal.db.SqlHandlerInDBTest;
import org.evomaster.client.java.controller.internal.db.mysql.DatabaseFakeMySQLSutController;
import org.evomaster.client.java.controller.internal.db.mysql.DatabaseMySQLTestInit;

import java.sql.Connection;

public class SqlHandlerInPostgresDBTest extends DatabasePostgresTestInit implements SqlHandlerInDBTest {
    @Override
    public Connection getConnection() {
        return connection;
    }

    @Override
    public SutController getSutController() {
        return new DatabaseFakeMySQLSutController(connection);
    }
}
