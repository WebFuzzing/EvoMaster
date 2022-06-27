package org.evomaster.client.java.controller.internal.db.postgres;

import org.evomaster.client.java.controller.internal.SutController;
import org.evomaster.client.java.controller.internal.db.HeuristicsCalculatorInDBTest;
import org.evomaster.client.java.controller.internal.db.mysql.DatabaseFakeMySQLSutController;
import org.evomaster.client.java.controller.internal.db.mysql.DatabaseMySQLTestInit;

import java.sql.Connection;

public class HeuristicsCalculatorInPostgresTest extends DatabasePostgresTestInit implements HeuristicsCalculatorInDBTest{

    @Override
    public Connection getConnection() {
        return connection;
    }

    @Override
    public SutController getSutController() {
        return new DatabaseFakePostgresSutController(connection);
    }
}
