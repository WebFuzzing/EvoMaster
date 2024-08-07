package org.evomaster.client.java.controller.internal.db.sql.mysql;

import org.evomaster.client.java.controller.internal.SutController;
import org.evomaster.client.java.controller.internal.db.sql.HeuristicsCalculatorWithSqlInsertionTest;

import java.sql.Connection;

public class HeuristicsCalculatorWithSqlInsertionMySQLDBTest extends DatabaseMySQLTestInit implements HeuristicsCalculatorWithSqlInsertionTest {

    @Override
    public Connection getConnection() {
        return connection;
    }

    @Override
    public SutController getSutController() {
        return new DatabaseFakeMySQLSutController(connection);
    }

}
