package org.evomaster.client.java.sql.heuristic;

import org.evomaster.client.java.controller.api.dto.database.execution.SqlExecutionLogDto;
import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType;
import org.evomaster.client.java.controller.api.dto.database.schema.DbInfoDto;
import org.evomaster.client.java.sql.internal.SqlCommandWithDistance;
import org.evomaster.client.java.sql.internal.SqlHandler;
import org.junit.jupiter.api.AfterEach;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class SqlHandlerGetDistanceTestBase {

    protected abstract Connection getConnection();

    protected abstract DatabaseType getDbType();

    protected abstract void clearDatabase() throws SQLException;

    protected DbInfoDto schema;

    @AfterEach
    public void dropAllTables() throws Exception {
        clearDatabase();
    }

    protected double computeSqlDistance(String sqlCommand, boolean completeSqlHeuristics) {
        final SqlExecutionLogDto sqlExecutionLogDto = new SqlExecutionLogDto(sqlCommand, false, 10L);
        SqlHandler sqlHandler = new SqlHandler(null);
        sqlHandler.setCompleteSqlHeuristics(completeSqlHeuristics);
        sqlHandler.setSchema(schema);
        sqlHandler.setConnection(getConnection());
        sqlHandler.handle(sqlExecutionLogDto);
        List<SqlCommandWithDistance> sqlDistances = sqlHandler.getSqlDistances(null, true);
        assertEquals(1, sqlDistances.size());
        SqlCommandWithDistance sqlCommandWithDistance = sqlDistances.iterator().next();
        final double sqlDistance = sqlCommandWithDistance.sqlDistanceWithMetrics.sqlDistance;
        return sqlDistance;
    }
}
