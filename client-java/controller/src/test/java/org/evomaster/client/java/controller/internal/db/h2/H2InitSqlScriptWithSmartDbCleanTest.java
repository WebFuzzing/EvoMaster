package org.evomaster.client.java.controller.internal.db.h2;

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType;
import org.evomaster.client.java.controller.db.DbCleaner;
import org.evomaster.client.java.controller.internal.SutController;
import org.evomaster.client.java.controller.internal.db.InitSqlScriptWithSmartDbCleanTest;
import org.evomaster.client.java.controller.internal.db.SmartDbCleanTest;

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
