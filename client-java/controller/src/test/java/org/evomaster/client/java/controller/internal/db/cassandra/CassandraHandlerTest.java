package org.evomaster.client.java.controller.internal.db.cassandra;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import org.evomaster.client.java.controller.api.dto.database.execution.CassandraExecutionsDto;
import org.evomaster.client.java.instrumentation.ExecutedCqlCommand;
import org.evomaster.client.java.instrumentation.cassandra.CassandraColumnMetadata;
import org.evomaster.client.java.instrumentation.cassandra.CassandraTableMetadata;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CassandraHandlerTest {

    private static final int CASSANDRA_PORT = 9042;
    private static final String CASSANDRA_IMAGE = "cassandra";
    private static final String CASSANDRA_VERSION = "4.1";

    private static final GenericContainer<?> cassandra = new GenericContainer<>(CASSANDRA_IMAGE + ":" + CASSANDRA_VERSION)
            .withExposedPorts(CASSANDRA_PORT)
            .waitingFor(Wait.forLogMessage(".*Startup complete.*\\n", 1))
            .withStartupTimeout(Duration.ofMinutes(3));

    private static CqlSession session;

    private static final String KEYSPACE = "my_keyspace";
    private static final String TABLE = "my_table";

    private CassandraHandler handler;

    @BeforeAll
    public static void initClass() {
        cassandra.start();

        session = CqlSession.builder()
                .addContactPoint(new InetSocketAddress(cassandra.getHost(), cassandra.getMappedPort(CASSANDRA_PORT)))
                .withLocalDatacenter("datacenter1")
                .build();

        session.execute("CREATE KEYSPACE IF NOT EXISTS " + KEYSPACE +
                " WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1}");
        session.execute("CREATE TABLE IF NOT EXISTS " + KEYSPACE + "." + TABLE +
                " (id int PRIMARY KEY, age int, name text)");
        session.execute("CREATE TABLE IF NOT EXISTS " + KEYSPACE + ".\"MyMixedCaseTable\"" +
                " (id int PRIMARY KEY, age int)");
    }

    @AfterAll
    public static void closeClass() {
        if (session != null) {
            session.close();
        }
        cassandra.stop();
    }

    @BeforeEach
    public void setUp() {
        session.execute("TRUNCATE " + KEYSPACE + "." + TABLE);
        session.execute("TRUNCATE " + KEYSPACE + ".\"MyMixedCaseTable\"");

        handler = new CassandraHandler();
        handler.setCqlSession(session);
    }

    private static ExecutedCqlCommand command(String cql) {
        return new ExecutedCqlCommand(cql, KEYSPACE, TABLE, false, 1);
    }

    private static CassandraTableMetadata tableSchema(String tableName) {
        return new CassandraTableMetadata(KEYSPACE, tableName,
                Collections.singletonList(new CassandraColumnMetadata("id", "int", true, false)));
    }

    @Test
    public void testSelectDistance_zeroWhenRowMatches() {
        session.execute("INSERT INTO " + KEYSPACE + "." + TABLE + " (id, age, name) VALUES (1, 30, 'John Doe')");

        String cql = "SELECT * FROM " + KEYSPACE + "." + TABLE + " WHERE age = 30";
        handler.handle(command(cql));

        List<CqlCommandWithDistance> results = handler.getEvaluatedCqlCommands();

        assertEquals(1, results.size());
        CqlCommandWithDistance result = results.get(0);
        assertEquals(cql, result.getCqlCommand());
        assertEquals(0.0, result.getCqlDistanceWithMetrics().getCqlDistance());
        assertEquals(1, result.getCqlDistanceWithMetrics().getNumberOfEvaluatedRows());
    }

    @Test
    public void testSelectDistance_nonZeroWhenNoRowMatches() {
        session.execute("INSERT INTO " + KEYSPACE + "." + TABLE + " (id, age, name) VALUES (1, 30, 'John Doe')");

        String cql = "SELECT * FROM " + KEYSPACE + "." + TABLE + " WHERE age = 18";
        handler.handle(command(cql));

        List<CqlCommandWithDistance> results = handler.getEvaluatedCqlCommands();

        assertEquals(1, results.size());
        assertTrue(results.get(0).getCqlDistanceWithMetrics().getCqlDistance() > 0);
        assertEquals(1, results.get(0).getCqlDistanceWithMetrics().getNumberOfEvaluatedRows());
    }

    @Test
    public void testUpdateDistance_evaluatedWithoutMutatingTheRow() {
        session.execute("INSERT INTO " + KEYSPACE + "." + TABLE + " (id, age, name) VALUES (1, 30, 'John Doe')");

        String cql = "UPDATE " + KEYSPACE + "." + TABLE + " SET name = 'Changed' WHERE id = 1";
        handler.handle(command(cql));

        List<CqlCommandWithDistance> results = handler.getEvaluatedCqlCommands();

        assertEquals(1, results.size());
        assertEquals(0.0, results.get(0).getCqlDistanceWithMetrics().getCqlDistance());

        // the handler must never actually run the UPDATE itself, only read the table
        ResultSet check = session.execute("SELECT name FROM " + KEYSPACE + "." + TABLE + " WHERE id = 1");
        assertEquals("John Doe", check.one().getString("name"));
    }

    @Test
    public void testDeleteDistance_evaluatedWithoutDeletingTheRow() {
        session.execute("INSERT INTO " + KEYSPACE + "." + TABLE + " (id, age, name) VALUES (1, 30, 'John Doe')");

        String cql = "DELETE FROM " + KEYSPACE + "." + TABLE + " WHERE id = 1";
        handler.handle(command(cql));

        List<CqlCommandWithDistance> results = handler.getEvaluatedCqlCommands();

        assertEquals(1, results.size());
        assertEquals(0.0, results.get(0).getCqlDistanceWithMetrics().getCqlDistance());

        // the handler must never actually run the DELETE itself, only read the table
        ResultSet check = session.execute("SELECT id FROM " + KEYSPACE + "." + TABLE + " WHERE id = 1 ALLOW FILTERING");
        assertNotNull(check.one());
    }

    @Test
    public void testInsertIsNotBufferedNorEvaluated() {
        String cql = "INSERT INTO " + KEYSPACE + "." + TABLE + " (id, age, name) VALUES (2, 40, 'Jane')";
        handler.handle(command(cql));

        List<CqlCommandWithDistance> results = handler.getEvaluatedCqlCommands();

        assertTrue(results.isEmpty());
    }

    @Test
    public void testEmptyTable_recordedAsFailedQueryWithSchema() {
        handler.handle(tableSchema(TABLE));

        String cql = "SELECT * FROM " + KEYSPACE + "." + TABLE + " WHERE id = 1";
        handler.handle(command(cql));
        handler.getEvaluatedCqlCommands();

        CassandraExecutionsDto dto = handler.getExecutionDto();

        assertEquals(1, dto.failedQueries.size());
        assertEquals(KEYSPACE, dto.failedQueries.get(0).getKeyspaceName());
        assertEquals(TABLE, dto.failedQueries.get(0).getTableName());
        assertEquals("id int PARTITION KEY", dto.failedQueries.get(0).getTableSchema());
    }

    /**
     * Regression test: the handler must re-derive the table reference from the parsed CQL AST
     * (which preserves quoting/case), not from a regex-stripped table name, otherwise it would
     * query the wrong (non-existent, lower-cased) table.
     */
    @Test
    public void testQuotedMixedCaseTableName_targetsTheCorrectTable() {
        session.execute("INSERT INTO " + KEYSPACE + ".\"MyMixedCaseTable\" (id, age) VALUES (1, 99)");

        String cql = "SELECT * FROM " + KEYSPACE + ".\"MyMixedCaseTable\" WHERE age = 99";
        // deliberately pass the regex-stripped (unquoted) table name, as instrumentation would:
        // the handler must ignore it and re-parse cqlCommand itself
        handler.handle(new ExecutedCqlCommand(cql, KEYSPACE, "MyMixedCaseTable", false, 1));

        List<CqlCommandWithDistance> results = handler.getEvaluatedCqlCommands();

        assertEquals(1, results.size());
        assertEquals(0.0, results.get(0).getCqlDistanceWithMetrics().getCqlDistance());
        assertEquals(1, results.get(0).getCqlDistanceWithMetrics().getNumberOfEvaluatedRows());
    }

    @Test
    public void testReset_clearsComputedDistances() {
        handler.handle(command("SELECT * FROM " + KEYSPACE + "." + TABLE + " WHERE id = 1"));
        assertEquals(1, handler.getEvaluatedCqlCommands().size());

        handler.reset();

        // without reset, the previous round's distance would still be in the returned list
        assertTrue(handler.getEvaluatedCqlCommands().isEmpty());
    }

    @Test
    public void testReset_clearsFailedQueries() {
        handler.handle(command("SELECT * FROM " + KEYSPACE + "." + TABLE + " WHERE id = 1"));
        handler.getEvaluatedCqlCommands();
        assertEquals(1, handler.getExecutionDto().failedQueries.size());

        handler.reset();

        assertTrue(handler.getExecutionDto().failedQueries.isEmpty());
    }

    @Test
    public void testExtractCqlExecutionDisabled_commandNotBuffered() {
        handler.setExtractCqlExecution(false);

        handler.handle(command("SELECT * FROM " + KEYSPACE + "." + TABLE + " WHERE id = 1"));

        assertTrue(handler.getEvaluatedCqlCommands().isEmpty());
    }

    @Test
    public void testExtractCqlExecutionDisabled_tableSchemaNotRecorded() {
        handler.setExtractCqlExecution(false);
        handler.handle(tableSchema(TABLE));

        // re-enable so the (still empty) table is actually evaluated and tracked as a failed query
        handler.setExtractCqlExecution(true);
        handler.handle(command("SELECT * FROM " + KEYSPACE + "." + TABLE + " WHERE id = 1"));
        handler.getEvaluatedCqlCommands();

        CassandraExecutionsDto dto = handler.getExecutionDto();

        assertEquals(1, dto.failedQueries.size());
        // the schema was never captured, since extractCqlExecution was disabled when it was offered
        assertNull(dto.failedQueries.get(0).getTableSchema());
    }

    @Test
    public void testMultipleQueriesEvaluatedInSameRound() {
        session.execute("INSERT INTO " + KEYSPACE + "." + TABLE + " (id, age, name) VALUES (1, 30, 'John Doe')");

        String matchingCql = "SELECT * FROM " + KEYSPACE + "." + TABLE + " WHERE age = 30";
        String nonMatchingCql = "SELECT * FROM " + KEYSPACE + "." + TABLE + " WHERE age = 18";
        handler.handle(command(matchingCql));
        handler.handle(command(nonMatchingCql));

        List<CqlCommandWithDistance> results = handler.getEvaluatedCqlCommands();

        assertEquals(2, results.size());
        assertEquals(matchingCql, results.get(0).getCqlCommand());
        assertEquals(0.0, results.get(0).getCqlDistanceWithMetrics().getCqlDistance());
        assertEquals(nonMatchingCql, results.get(1).getCqlCommand());
        assertTrue(results.get(1).getCqlDistanceWithMetrics().getCqlDistance() > 0.0);
    }

    @Test
    public void testMultipleRows_allRowsFetchedAndBestRowWins() {
        session.execute("INSERT INTO " + KEYSPACE + "." + TABLE + " (id, age, name) VALUES (1, 10, 'A')");
        session.execute("INSERT INTO " + KEYSPACE + "." + TABLE + " (id, age, name) VALUES (2, 30, 'B')");
        session.execute("INSERT INTO " + KEYSPACE + "." + TABLE + " (id, age, name) VALUES (3, 50, 'C')");

        handler.handle(command("SELECT * FROM " + KEYSPACE + "." + TABLE + " WHERE age = 30"));
        List<CqlCommandWithDistance> results = handler.getEvaluatedCqlCommands();

        assertEquals(1, results.size());
        assertEquals(0.0, results.get(0).getCqlDistanceWithMetrics().getCqlDistance());
        assertEquals(3, results.get(0).getCqlDistanceWithMetrics().getNumberOfEvaluatedRows());
    }

    @Test
    public void testCloserMismatchYieldsSmallerDistance() {
        session.execute("INSERT INTO " + KEYSPACE + "." + TABLE + " (id, age, name) VALUES (1, 30, 'John Doe')");

        handler.handle(command("SELECT * FROM " + KEYSPACE + "." + TABLE + " WHERE age = 29"));
        handler.handle(command("SELECT * FROM " + KEYSPACE + "." + TABLE + " WHERE age = 1000"));

        List<CqlCommandWithDistance> results = handler.getEvaluatedCqlCommands();
        double closeDistance = results.get(0).getCqlDistanceWithMetrics().getCqlDistance();
        double farDistance = results.get(1).getCqlDistanceWithMetrics().getCqlDistance();

        assertTrue(closeDistance > 0.0);
        assertTrue(farDistance < 1.0);
        assertTrue(closeDistance < farDistance);
    }

    @Test
    public void testMalformedSelectCommand_throws() {
        // starts with SELECT (so it is buffered) but is not valid CQL beyond that: the table
        // reference can't be resolved, and that's not an expected/routine outcome, so it throws
        handler.handle(command("SELECT"));

        assertThrows(RuntimeException.class, handler::getEvaluatedCqlCommands);
    }

    @Test
    public void testEmptyTable_noSchemaKnown_tableSchemaIsNull() {
        handler.handle(command("SELECT * FROM " + KEYSPACE + "." + TABLE + " WHERE id = 1"));
        handler.getEvaluatedCqlCommands();

        CassandraExecutionsDto dto = handler.getExecutionDto();

        assertEquals(1, dto.failedQueries.size());
        assertNull(dto.failedQueries.get(0).getTableSchema());
    }

    @Test
    public void testNonEmptyTable_noRowMatches_recordedAsFailedQuery() {
        session.execute("INSERT INTO " + KEYSPACE + "." + TABLE + " (id, age, name) VALUES (1, 30, 'John Doe')");

        // the table itself is not empty, but no row satisfies the WHERE clause
        handler.handle(command("SELECT * FROM " + KEYSPACE + "." + TABLE + " WHERE age = 18"));
        handler.getEvaluatedCqlCommands();

        assertEquals(1, handler.getExecutionDto().failedQueries.size());
    }

    @Test
    public void testNonEmptyTable_rowMatches_notRecordedAsFailedQuery() {
        session.execute("INSERT INTO " + KEYSPACE + "." + TABLE + " (id, age, name) VALUES (1, 30, 'John Doe')");

        handler.handle(command("SELECT * FROM " + KEYSPACE + "." + TABLE + " WHERE age = 30"));
        handler.getEvaluatedCqlCommands();

        assertTrue(handler.getExecutionDto().failedQueries.isEmpty());
    }

    @Test
    public void testInOperator_endToEnd() {
        session.execute("INSERT INTO " + KEYSPACE + "." + TABLE + " (id, age, name) VALUES (1, 30, 'John Doe')");

        String cql = "SELECT * FROM " + KEYSPACE + "." + TABLE + " WHERE age IN (18, 25, 30)";
        handler.handle(command(cql));

        List<CqlCommandWithDistance> results = handler.getEvaluatedCqlCommands();

        assertEquals(1, results.size());
        assertEquals(0.0, results.get(0).getCqlDistanceWithMetrics().getCqlDistance());
    }

    @Test
    public void testAndOperator_endToEnd() {
        session.execute("INSERT INTO " + KEYSPACE + "." + TABLE + " (id, age, name) VALUES (1, 30, 'John Doe')");

        String matchingCql = "SELECT * FROM " + KEYSPACE + "." + TABLE + " WHERE age = 30 AND name = 'John Doe'";
        String partialMismatchCql = "SELECT * FROM " + KEYSPACE + "." + TABLE + " WHERE age = 30 AND name = 'Someone Else'";
        handler.handle(command(matchingCql));
        handler.handle(command(partialMismatchCql));

        List<CqlCommandWithDistance> results = handler.getEvaluatedCqlCommands();

        assertEquals(2, results.size());
        assertEquals(0.0, results.get(0).getCqlDistanceWithMetrics().getCqlDistance());
        assertTrue(results.get(1).getCqlDistanceWithMetrics().getCqlDistance() > 0.0);
    }

    @Test
    public void testNoCqlSessionSet_throwsIllegalStateException() {
        CassandraHandler handlerWithoutSession = new CassandraHandler();

        handlerWithoutSession.handle(command("SELECT * FROM " + KEYSPACE + "." + TABLE + " WHERE id = 1"));

        assertThrows(IllegalStateException.class, handlerWithoutSession::getEvaluatedCqlCommands);
    }

    @Test
    public void testHandleExecutedCqlCommand_nullInfo_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> handler.handle((ExecutedCqlCommand) null));
    }

    @Test
    public void testHandleExecutedCqlCommand_nullCqlCommand_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
                () -> handler.handle(new ExecutedCqlCommand(null, KEYSPACE, TABLE, false, 1)));
    }

    @Test
    public void testHandleTableSchema_nullInfo_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> handler.handle((CassandraTableMetadata) null));
    }
}