package org.evomaster.client.java.sql.internal;

import org.evomaster.client.java.sql.DataRow;
import org.evomaster.client.java.sql.QueryResult;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SqlHeuristicsCalculatorTest {

    @Test
    public void testNoWhereNoFromClause() {
        String sqlCommand = "SELECT 1 AS example_column";
        QueryResult data = new QueryResult(Arrays.asList("example_column"), null);
        data.addRow(new DataRow("example_column",1, null));
        SqlDistanceWithMetrics distanceWithMetrics = SqlHeuristicsCalculator.computeDistance(sqlCommand, null, null, data);
        assertEquals(0, distanceWithMetrics.sqlDistance);
    }
}
