package org.evomaster.client.java.controller.internal.db.sql.h2;

import org.evomaster.client.java.controller.internal.SutController;
import org.evomaster.client.java.controller.internal.db.sql.HeuristicsCalculatorWithSqlInsertionTest;

import java.sql.Connection;

public class HeuristicsCalculatorWithSqlInsertionH2DBTest extends DatabaseH2TestInit implements HeuristicsCalculatorWithSqlInsertionTest {

    @Override
    public Connection getConnection() {
        return connection;
    }

    @Override
    public SutController getSutController() {
        return new DatabaseFakeH2SutController(connection);
    }


}
