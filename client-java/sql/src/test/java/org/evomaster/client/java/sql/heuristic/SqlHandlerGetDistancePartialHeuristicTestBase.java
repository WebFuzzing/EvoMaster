package org.evomaster.client.java.sql.heuristic;

import org.evomaster.client.java.sql.DbInfoExtractor;
import org.evomaster.client.java.sql.SqlScriptRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class SqlHandlerGetDistancePartialHeuristicTestBase extends SqlHandlerGetDistanceTestBase {


    @BeforeEach
    public void createAllTables() throws Exception {
        schema = DbInfoExtractor.extract(getConnection());
    }


    private double computeSqlDistance(String sqlCommand) {
        return super.computeSqlDistance(sqlCommand, false);
    }

    @Test
    public void testDeleteWithNullComparison() throws Exception {
        SqlScriptRunner.execCommand(getConnection(), "CREATE TABLE foo (column0 INT, column1 INT)");
        try {
            SqlScriptRunner.execCommand(getConnection(), "INSERT INTO foo VALUES (1, 1)");

            // Refresh schema because we added a new table
            schema = DbInfoExtractor.extract(getConnection());

            final double sqlDistance = computeSqlDistance("DELETE FROM foo WHERE column0 = NULL OR column1 = NULL");
            assertTrue(sqlDistance >= 0);
        } finally {
            SqlScriptRunner.execCommand(getConnection(), "DROP TABLE foo");
        }
    }
}
