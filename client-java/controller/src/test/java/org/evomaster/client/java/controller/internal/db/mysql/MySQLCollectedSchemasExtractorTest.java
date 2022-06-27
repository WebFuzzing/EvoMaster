package org.evomaster.client.java.controller.internal.db.mysql;

import org.evomaster.client.java.controller.internal.SutController;
import org.evomaster.client.java.controller.db.CollectedSchemasExtractorTestBase;

import java.sql.Connection;

public class MySQLCollectedSchemasExtractorTest extends DatabaseMySQLTestInit implements CollectedSchemasExtractorTestBase {

    @Override
    public Connection getConnection() {
        return connection;
    }

}
