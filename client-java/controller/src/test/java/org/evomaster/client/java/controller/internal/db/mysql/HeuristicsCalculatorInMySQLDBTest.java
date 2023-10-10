package org.evomaster.client.java.controller.internal.db.mysql;

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType;
import org.evomaster.client.java.controller.db.DbCleaner;
import org.evomaster.client.java.controller.internal.SutController;
import org.evomaster.client.java.controller.internal.db.HeuristicsCalculatorInDBTest;

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
