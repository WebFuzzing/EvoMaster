package org.evomaster.client.java.controller.internal.db.sql.mysql;

import org.evomaster.client.java.controller.internal.SutController;
import org.evomaster.client.java.controller.internal.db.sql.InitSqlScriptWithSmartDbCleanTest;

import java.sql.Connection;

public class MySQLInitSqlScriptWithSmartDbCleanTest extends DatabaseMySQLTestInit implements InitSqlScriptWithSmartDbCleanTest {

    @Override
    public Connection getConnection() {
        return connection;
    }

    @Override
    public SutController getSutController() {
        return new DatabaseFakeMySQLSutController(connection, getInitSqlScript());
    }

}
