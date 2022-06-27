package org.evomaster.client.java.controller.internal.db.postgres;

import org.evomaster.client.java.controller.internal.SutController;
import org.evomaster.client.java.controller.db.CollectedSchemasExtractorTestBase;

import java.sql.Connection;

public class PostgresCollectedSchemasExtractorTest extends DatabasePostgresTestInit implements CollectedSchemasExtractorTestBase {

    @Override
    public Connection getConnection() {
        return connection;
    }

}
