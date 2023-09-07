package org.evomaster.client.java.controller.internal.db.mysql;

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType;
import org.evomaster.client.java.controller.db.DbCleaner;
import org.evomaster.client.java.controller.internal.SutController;
import org.evomaster.client.java.controller.internal.db.InitSqlScriptWithSmartDbCleanTest;
import org.evomaster.client.java.controller.internal.db.SmartDbCleanTest;

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
