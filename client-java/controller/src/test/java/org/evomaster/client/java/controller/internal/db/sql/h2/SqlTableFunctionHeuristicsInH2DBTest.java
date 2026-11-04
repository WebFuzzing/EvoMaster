package org.evomaster.client.java.controller.internal.db.sql.h2;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.evomaster.client.java.controller.DatabaseTestTemplate;
import org.evomaster.client.java.controller.InstrumentedSutStarter;
import org.evomaster.client.java.controller.api.ControllerConstants;
import org.evomaster.client.java.controller.api.dto.ExtraHeuristicEntryDto;
import org.evomaster.client.java.controller.api.dto.SutRunDto;
import org.evomaster.client.java.controller.api.dto.TestResultsDto;
import org.evomaster.client.java.controller.internal.SutController;
import org.evomaster.client.java.controller.internal.db.sql.EMSqlScriptRunner;
import org.evomaster.client.java.sql.SqlScriptRunner;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.evomaster.client.java.controller.api.ControllerConstants.BASE_PATH;
import static org.evomaster.client.java.controller.api.ControllerConstants.RUN_SUT_PATH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reproduces the loss of coverage observed on the "jasper" case study.
 *
 * Jasper issues SELECTs of the shape
 *
 *      ... CROSS JOIN LATERAL jsonb_array_elements_text(...) AS t(x) ...
 *
 * ie, a FROM item that JSqlParser represents as a
 * {@link net.sf.jsqlparser.statement.select.TableFunction}. Only 2 distinct queries of that
 * shape are enough to disrupt a whole search.
 *
 * The partial (default) SQL heuristics have no support for table functions: the visitors in
 * {@code SqlNameContext} are deliberate stubs that just {@code throw new UnsupportedOperationException()}
 * (no message, hence the "Thrown exception: null" seen in the error body). The problem is not the
 * missing support in itself, but that the exception is not contained:
 *
 * <ul>
 *   <li>{@code SutController#computeSQLHeuristics} calls {@code sqlHandler.handle(...)} inside a
 *       try/catch, so a failure there is only logged as "FAILED TO HANDLE SQL COMMAND";</li>
 *   <li>but the sibling call {@code sqlHandler.getSqlDistances(...)} a few lines below has no
 *       try/catch at all.</li>
 * </ul>
 *
 * Since the SQL heuristics travel in the very same HTTP response as the coverage
 * ({@code EMController#getTestResults}), the escaping exception becomes a 500. EvoMaster then
 * discards the individual and restarts the SUT, and on restart the SUT renumbers its targets,
 * so the same logical target ends up being counted twice.
 *
 * H2 has no PostgreSQL {@code jsonb_array_elements_text}, but {@code UNNEST} produces exactly the
 * same parse tree (a CROSS JOIN whose right item is a TableFunction), which is what the heuristics
 * choke on.
 *
 * NOTE: these tests document the CURRENT behaviour of the production code, which is left untouched.
 * The first one is expected to FAIL: that failure IS the bug.
 *
 * NOTE on Java assertions: the catch protecting {@code handle(...)} ends with {@code assert false}.
 * When the test JVM runs with -ea (the surefire default) that AssertionError is what escapes first,
 * and the 500 is served by Jetty. A real driver runs without -ea, and then the 500 comes from the
 * catch in {@code EMController} itself, reporting the uncaught {@code UnsupportedOperationException}
 * thrown at {@code SqlNameContext.visit(TableFunction)} from inside {@code getSqlDistances}. Either
 * way the status code is 500; run with {@code -DenableAssertions=false} to see the production stack
 * trace.
 */
public class SqlTableFunctionHeuristicsInH2DBTest extends DatabaseH2TestInit implements DatabaseTestTemplate {

    /**
     * Same shape as the jasper queries: a CROSS JOIN over a table function, plus a WHERE on a
     * column of a regular table, so that the heuristics do need to resolve table names and aliases.
     */
    private static final String SELECT_WITH_TABLE_FUNCTION =
            "SELECT f.id, v.c FROM foo f CROSS JOIN UNNEST(ARRAY['a','b']) AS v(c) WHERE f.id > 0";

    private static final String SELECT_WITHOUT_TABLE_FUNCTION =
            "SELECT f.id FROM foo f WHERE f.id > 0";


    /**
     * This is the bug: the endpoint EvoMaster uses to collect the coverage of a test answers 500,
     * only because the SQL heuristics cannot parse a table function.
     */
    @Test
    public void testTableFunctionDoesNotBreakTestResultsEndpoint() throws Exception {

        SqlScriptRunner.execCommand(getConnection(), "CREATE TABLE foo (id INT)");

        InstrumentedSutStarter starter = getInstrumentedSutStarter();
        try {
            String url = startSutForNewTest(starter, false);

            //control: with a query the heuristics do understand, the endpoint is perfectly fine
            EMSqlScriptRunner.execCommand(getConnection(), SELECT_WITHOUT_TABLE_FUNCTION, true);
            assertEquals(200, getTestResults(url).statusCode());

            //now the very same setup, only with a table function in the FROM
            startNewActionInSameTest(url, 1);
            EMSqlScriptRunner.execCommand(getConnection(), SELECT_WITH_TABLE_FUNCTION, true);

            Response response = getTestResults(url);

            assertEquals(200, response.statusCode(),
                    "Coverage endpoint broke on an unsupported SQL table function: "
                            + response.body().asString());
        } finally {
            starter.stop();
        }
    }

    /**
     * Exact same scenario, the only difference being {@code advancedHeuristics=true}, ie the
     * {@code heuristicsForSQLAdvanced} option on the EvoMaster side.
     *
     * That code path does not support table functions either ({@code SqlHeuristicsCalculator}
     * throws "Must implement TableFunction for computing heuristics"), but it catches its own
     * exceptions and falls back to a MAX_VALUE distance flagged as an evaluation failure. So the
     * response stays a 200, the individual is kept, and the SUT is not restarted.
     */
    @Test
    public void testTableFunctionWithAdvancedHeuristicsDoesNotBreakTestResultsEndpoint() throws Exception {

        SqlScriptRunner.execCommand(getConnection(), "CREATE TABLE foo (id INT)");

        InstrumentedSutStarter starter = getInstrumentedSutStarter();
        try {
            String url = startSutForNewTest(starter, true);

            EMSqlScriptRunner.execCommand(getConnection(), SELECT_WITH_TABLE_FUNCTION, true);

            Response response = getTestResults(url);

            assertEquals(200, response.statusCode(), response.body().asString());

            /*
                the heuristic is still reported, but marked as a failed evaluation:
                that is the exception being contained instead of escaping
             */
            TestResultsDto dto = response.body().jsonPath().getObject("data", TestResultsDto.class);
            assertNotNull(dto.extraHeuristics);
            assertEquals(1, dto.extraHeuristics.size());

            List<ExtraHeuristicEntryDto> sqlHeuristics = dto.extraHeuristics.get(0).heuristics;
            assertEquals(1, sqlHeuristics.size());

            ExtraHeuristicEntryDto entry = sqlHeuristics.get(0);
            assertEquals(ExtraHeuristicEntryDto.Type.SQL, entry.type);
            assertTrue(entry.extraHeuristicEvaluationFailure,
                    "the table function should be reported as a failed heuristic evaluation");
            assertEquals(Double.MAX_VALUE, entry.value);
        } finally {
            starter.stop();
        }
    }


    private String startSutForNewTest(InstrumentedSutStarter starter, boolean advancedHeuristics) {

        String url = start(starter) + BASE_PATH;

        SutRunDto dto = new SutRunDto(true, true, false, true, "BASE,SQL");
        dto.advancedHeuristics = advancedHeuristics;

        given().accept(ContentType.ANY)
                .contentType(ContentType.JSON)
                .body(dto)
                .put(url + RUN_SUT_PATH)
                .then()
                .statusCode(204);

        startNewActionInSameTest(url, 0);

        return url;
    }

    private Response getTestResults(String url) {
        return RestAssured.given().accept(ContentType.JSON)
                .get(url + ControllerConstants.TEST_RESULTS);
    }


    @Override
    public Connection getConnection() {
        return connection;
    }

    @Override
    public SutController getSutController() {
        return new DatabaseFakeH2SutController(connection);
    }
}
