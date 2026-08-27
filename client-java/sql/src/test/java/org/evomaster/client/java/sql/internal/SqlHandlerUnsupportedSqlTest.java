package org.evomaster.client.java.sql.internal;

import org.evomaster.client.java.controller.api.dto.database.execution.SqlExecutionLogDto;
import org.evomaster.client.java.controller.api.dto.database.schema.DbInfoDto;
import org.evomaster.client.java.sql.DbInfoExtractor;
import org.evomaster.client.java.sql.SqlScriptRunner;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SQL constructs that EvoMaster cannot analyze must not break the computation of
 * the heuristics for the other SQL commands.
 * <p>
 * There are several SQL constructs for which support was never implemented, and which
 * are represented with an explicit {@code throw new UnsupportedOperationException()},
 * eg in {@code SqlNameContext}: table functions, lateral sub-selects, parenthesized
 * from-items and nested selects.
 * <p>
 * This matters well beyond the heuristics themselves: the heuristics are computed inside
 * the same HTTP request that returns the coverage of the SUT. An exception escaping from
 * here makes that whole request fail, so EvoMaster does not just lose one heuristic, it
 * loses the coverage of the entire test, discards the individual, and restarts the SUT.
 * <p>
 * Note that {@code handle} is already protected by a try/catch on the caller side, in
 * {@code SutController.computeExtraHeuristics}, but {@code getSqlDistances} is not. The
 * SQL command is added to the buffer before the analysis that can throw, so a command
 * that made {@code handle} fail is still processed later by {@code getSqlDistances}.
 * These tests reproduce that exact sequence.
 */
public class SqlHandlerUnsupportedSqlTest {

    private static Connection connection;

    private static DbInfoDto schema;

    /**
     * A plain query that we do support, used to check that an unanalyzable command
     * does not take the others down with it.
     */
    private static final String SUPPORTED_QUERY = "SELECT column0 FROM foo WHERE column0 = 42";

    /**
     * Taken from the Jasper SUT. {@code jsonb_array_elements_text(...)} is a table function
     * used in a lateral join, which is what jsqlparser represents as a {@code TableFunction}.
     */
    private static final String LATERAL_TABLE_FUNCTION_QUERY =
            "SELECT DISTINCT t.tag FROM ref r " +
                    "CROSS JOIN LATERAL jsonb_array_elements_text(" +
                    "COALESCE(r.metadata->'expandedTags', r.tags)) AS t(tag) " +
                    "WHERE r.origin = ''";

    @BeforeAll
    public static void initClass() throws Exception {
        connection = DriverManager.getConnection("jdbc:h2:mem:db_test_unsupported_sql", "sa", "");
        SqlScriptRunner.execCommand(connection, "CREATE TABLE foo (column0 INT, column1 INT)");
        SqlScriptRunner.execCommand(connection, "INSERT INTO foo VALUES (42, 1)");
        schema = DbInfoExtractor.extract(connection);
    }

    @AfterAll
    public static void afterClass() throws Exception {
        connection.close();
    }

    private SqlHandler makeHandler(boolean completeSqlHeuristics) {
        SqlHandler sqlHandler = new SqlHandler(null);
        sqlHandler.setCompleteSqlHeuristics(completeSqlHeuristics);
        sqlHandler.setSchema(schema);
        sqlHandler.setConnection(connection);
        return sqlHandler;
    }

    /**
     * Mirrors what the driver does: {@code handle} is called inside a try/catch, as in
     * {@code SutController.computeExtraHeuristics}. The command still ends up buffered.
     */
    private void handleAsTheDriverDoes(SqlHandler sqlHandler, String sqlCommand) {
        try {
            sqlHandler.handle(new SqlExecutionLogDto(sqlCommand, false, 10L));
        } catch (Exception e) {
            //the driver logs it and keeps going
        }
    }

    @Test
    public void testUnsupportedSqlIsReportedAsEvaluationFailure() {

        SqlHandler sqlHandler = makeHandler(false);
        handleAsTheDriverDoes(sqlHandler, LATERAL_TABLE_FUNCTION_QUERY);

        //this is the call that must not throw
        List<SqlCommandWithDistance> distances = sqlHandler.getSqlDistances(null, true);

        assertEquals(1, distances.size());

        SqlDistanceWithMetrics metrics = distances.get(0).sqlDistanceWithMetrics;
        assertTrue(metrics.sqlDistanceEvaluationFailure,
                "An unanalyzable SQL command must be marked as an evaluation failure");
        assertEquals(Double.MAX_VALUE, metrics.sqlDistance);
    }

    @Test
    public void testUnsupportedSqlDoesNotDiscardTheOtherCommands() {

        SqlHandler sqlHandler = makeHandler(false);
        handleAsTheDriverDoes(sqlHandler, LATERAL_TABLE_FUNCTION_QUERY);
        handleAsTheDriverDoes(sqlHandler, SUPPORTED_QUERY);

        List<SqlCommandWithDistance> distances = sqlHandler.getSqlDistances(null, true);

        //the point of the fix: one unanalyzable command must not cost us the others
        assertEquals(2, distances.size());

        SqlCommandWithDistance forSupported = distances.stream()
                .filter(d -> d.sqlCommand.equals(SUPPORTED_QUERY))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Lost the heuristic of a command we do support"));

        assertFalse(forSupported.sqlDistanceWithMetrics.sqlDistanceEvaluationFailure);
    }
}
