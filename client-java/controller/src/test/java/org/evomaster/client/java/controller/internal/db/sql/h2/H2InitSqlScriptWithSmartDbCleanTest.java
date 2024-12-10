package org.evomaster.client.java.controller.internal.db.sql.h2;

import org.evomaster.client.java.controller.internal.SutController;
import org.evomaster.client.java.controller.internal.db.sql.InitSqlScriptWithSmartDbCleanTest;

import java.sql.Connection;

public class H2InitSqlScriptWithSmartDbCleanTest extends DatabaseH2TestInit implements InitSqlScriptWithSmartDbCleanTest {

    @Override
    public Connection getConnection() {
        return connection;
    }

    @Override
    public SutController getSutController() {
        return new DatabaseFakeH2SutController(connection, getInitSqlScript());
    }

}
