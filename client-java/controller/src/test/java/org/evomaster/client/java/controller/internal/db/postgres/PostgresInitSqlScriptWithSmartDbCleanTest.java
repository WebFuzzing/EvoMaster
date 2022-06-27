package org.evomaster.client.java.controller.internal.db.postgres;

import org.evomaster.client.java.controller.internal.SutController;
import org.evomaster.client.java.controller.internal.db.InitSqlScriptWithSmartDbCleanTest;
import org.evomaster.client.java.controller.internal.db.mysql.DatabaseFakeMySQLSutController;
import org.evomaster.client.java.controller.internal.db.mysql.DatabaseMySQLTestInit;

import java.sql.Connection;

public class PostgresInitSqlScriptWithSmartDbCleanTest extends DatabaseMySQLTestInit implements InitSqlScriptWithSmartDbCleanTest {

    @Override
    public Connection getConnection() {
        return connection;
    }

    @Override
    public SutController getSutController() {
        return new DatabaseFakeMySQLSutController(connection, getInitSqlScript());
    }
}
