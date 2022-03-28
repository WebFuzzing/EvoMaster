package org.evomaster.client.java.controller.internal.db.h2;

import org.evomaster.client.java.controller.internal.SutController;
import org.evomaster.client.java.controller.internal.db.HeuristicsCalculatorInDBTest;

import java.sql.Connection;

public class HeuristicsCalculatorInH2DBTest extends DatabaseH2TestInit implements HeuristicsCalculatorInDBTest {

    @Override
    public Connection getConnection() {
        return connection;
    }

    @Override
    public SutController getSutController() {
        return new DatabaseFakeH2SutController(connection);
    }
}