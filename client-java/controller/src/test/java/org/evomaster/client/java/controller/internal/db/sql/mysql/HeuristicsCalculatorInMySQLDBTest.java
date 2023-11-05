package org.evomaster.client.java.controller.internal.db.sql.mysql;

import org.evomaster.client.java.controller.internal.SutController;
import org.evomaster.client.java.controller.internal.db.sql.HeuristicsCalculatorInDBTest;

import java.sql.Connection;

public class HeuristicsCalculatorInMySQLDBTest extends DatabaseMySQLTestInit implements HeuristicsCalculatorInDBTest{

    @Override
    public Connection getConnection() {
        return connection;
    }

    @Override
    public SutController getSutController() {
        return new DatabaseFakeMySQLSutController(connection);
    }

}
