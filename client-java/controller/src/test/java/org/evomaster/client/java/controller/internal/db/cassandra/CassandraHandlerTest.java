package org.evomaster.client.java.controller.internal.db.cassandra;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import org.evomaster.client.java.controller.api.dto.database.execution.CassandraExecutionsDto;
import org.evomaster.client.java.instrumentation.CassandraTableSchema;
import org.evomaster.client.java.instrumentation.ExecutedCqlCommand;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CassandraHandlerTest {

    private static final int CASSANDRA_PORT = 9042;
    public static final String IMAGE_NAME = "cassandra:4.1";

    private static final GenericContainer<?> cassandra = new GenericContainer<>(IMAGE_NAME)
            .withExposedPorts(CASSANDRA_PORT)
            .waitingFor(Wait.forLogMessage(".*Startup complete.*\\n", 1))
            .withStartupTimeout(Duration.ofMinutes(3));

    private static CqlSession session;

    private static final String KEYSPACE = "my_keyspace";
    private static final String TABLE = "my_table";

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
    public void clearTables() {
        session.execute("TRUNCATE " + KEYSPACE + "." + TABLE);
        session.execute("TRUNCATE " + KEYSPACE + ".\"MyMixedCaseTable\"");
    }

    private static ExecutedCqlCommand command(String cql) {
        return new ExecutedCqlCommand(cql, KEYSPACE, TABLE, false, 1);
    }

    @Test
    public void testSelectDistance_zeroWhenRowMatches() {
        session.execute("INSERT INTO " + KEYSPACE + "." + TABLE + " (id, age, name) VALUES (1, 30, 'John Doe')");

        CassandraHandler handler = new CassandraHandler();
        handler.setCqlSession(session);

        String cql = "SELECT * FROM " + KEYSPACE + "." + TABLE + " WHERE age = 30";
        handler.handle(command(cql));

        List<CqlCommandWithDistance> results = handler.getEvaluatedCqlCommands();

        assertEquals(1, results.size());
        CqlCommandWithDistance result = results.get(0);
        assertEquals(cql, result.cqlCommand);
        assertEquals(0.0, result.cqlDistanceWithMetrics.cqlDistance);
        assertEquals(1, result.cqlDistanceWithMetrics.numberOfEvaluatedRows);
    }

    @Test
    public void testSelectDistance_nonZeroWhenNoRowMatches() {
        session.execute("INSERT INTO " + KEYSPACE + "." + TABLE + " (id, age, name) VALUES (1, 30, 'John Doe')");

        CassandraHandler handler = new CassandraHandler();
        handler.setCqlSession(session);

        String cql = "SELECT * FROM " + KEYSPACE + "." + TABLE + " WHERE age = 18";
        handler.handle(command(cql));

        List<CqlCommandWithDistance> results = handler.getEvaluatedCqlCommands();

        assertEquals(1, results.size());
        assertTrue(results.get(0).cqlDistanceWithMetrics.cqlDistance > 0);
        assertEquals(1, results.get(0).cqlDistanceWithMetrics.numberOfEvaluatedRows);
    }

    @Test
    public void testUpdateDistance_evaluatedWithoutMutatingTheRow() {
        session.execute("INSERT INTO " + KEYSPACE + "." + TABLE + " (id, age, name) VALUES (1, 30, 'John Doe')");

        CassandraHandler handler = new CassandraHandler();
        handler.setCqlSession(session);

        String cql = "UPDATE " + KEYSPACE + "." + TABLE + " SET name = 'Changed' WHERE id = 1";
        handler.handle(command(cql));

        List<CqlCommandWithDistance> results = handler.getEvaluatedCqlCommands();

        assertEquals(1, results.size());
        assertEquals(0.0, results.get(0).cqlDistanceWithMetrics.cqlDistance);

        // the handler must never actually run the UPDATE itself, only read the table
        ResultSet check = session.execute("SELECT name FROM " + KEYSPACE + "." + TABLE + " WHERE id = 1");
        assertEquals("John Doe", check.one().getString("name"));
    }

    @Test
    public void testDeleteDistance_evaluatedWithoutDeletingTheRow() {
        session.execute("INSERT INTO " + KEYSPACE + "." + TABLE + " (id, age, name) VALUES (1, 30, 'John Doe')");

        CassandraHandler handler = new CassandraHandler();
        handler.setCqlSession(session);

        String cql = "DELETE FROM " + KEYSPACE + "." + TABLE + " WHERE id = 1";
        handler.handle(command(cql));

        List<CqlCommandWithDistance> results = handler.getEvaluatedCqlCommands();

        assertEquals(1, results.size());
        assertEquals(0.0, results.get(0).cqlDistanceWithMetrics.cqlDistance);

        // the handler must never actually run the DELETE itself, only read the table
        ResultSet check = session.execute("SELECT id FROM " + KEYSPACE + "." + TABLE + " WHERE id = 1 ALLOW FILTERING");
        assertNotNull(check.one());
    }

    @Test
    public void testInsertIsNotBufferedNorEvaluated() {
        CassandraHandler handler = new CassandraHandler();
        handler.setCqlSession(session);

        String cql = "INSERT INTO " + KEYSPACE + "." + TABLE + " (id, age, name) VALUES (2, 40, 'Jane')";
        handler.handle(command(cql));

        List<CqlCommandWithDistance> results = handler.getEvaluatedCqlCommands();

        assertTrue(results.isEmpty());
    }

    @Test
    public void testEmptyTable_recordedAsFailedQueryWithSchema() {
        CassandraHandler handler = new CassandraHandler();
        handler.setCqlSession(session);
        handler.handle(new CassandraTableSchema(TABLE, "{\"id\":\"int\"}"));

        String cql = "SELECT * FROM " + KEYSPACE + "." + TABLE + " WHERE id = 1";
        handler.handle(command(cql));
        handler.getEvaluatedCqlCommands();

        CassandraExecutionsDto dto = handler.getExecutionDto();

        assertEquals(1, dto.failedQueries.size());
        assertEquals(KEYSPACE, dto.failedQueries.get(0).getKeyspaceName());
        assertEquals(TABLE, dto.failedQueries.get(0).getTableName());
        assertEquals("{\"id\":\"int\"}", dto.failedQueries.get(0).getTableSchema());
    }

    /**
     * Regression test: the handler must re-derive the table reference from the parsed CQL AST
     * (which preserves quoting/case), not from a regex-stripped table name, otherwise it would
     * query the wrong (non-existent, lower-cased) table.
     */
    @Test
    public void testQuotedMixedCaseTableName_targetsTheCorrectTable() {
        session.execute("INSERT INTO " + KEYSPACE + ".\"MyMixedCaseTable\" (id, age) VALUES (1, 99)");

        CassandraHandler handler = new CassandraHandler();
        handler.setCqlSession(session);

        String cql = "SELECT * FROM " + KEYSPACE + ".\"MyMixedCaseTable\" WHERE age = 99";
        // deliberately pass the regex-stripped (unquoted) table name, as instrumentation would:
        // the handler must ignore it and re-parse cqlCommand itself
        handler.handle(new ExecutedCqlCommand(cql, KEYSPACE, "MyMixedCaseTable", false, 1));

        List<CqlCommandWithDistance> results = handler.getEvaluatedCqlCommands();

        assertEquals(1, results.size());
        assertEquals(0.0, results.get(0).cqlDistanceWithMetrics.cqlDistance);
        assertEquals(1, results.get(0).cqlDistanceWithMetrics.numberOfEvaluatedRows);
    }

    @Test
    public void testReset_clearsComputedDistances() {
        CassandraHandler handler = new CassandraHandler();
        handler.setCqlSession(session);

        handler.handle(command("SELECT * FROM " + KEYSPACE + "." + TABLE + " WHERE id = 1"));
        assertEquals(1, handler.getEvaluatedCqlCommands().size());

        handler.reset();

        // without reset, the previous round's distance would still be in the returned list
        assertTrue(handler.getEvaluatedCqlCommands().isEmpty());
    }

    @Test
    public void testReset_clearsFailedQueries() {
        CassandraHandler handler = new CassandraHandler();
        handler.setCqlSession(session);

        handler.handle(command("SELECT * FROM " + KEYSPACE + "." + TABLE + " WHERE id = 1"));
        handler.getEvaluatedCqlCommands();
        assertEquals(1, handler.getExecutionDto().failedQueries.size());

        handler.reset();

        assertTrue(handler.getExecutionDto().failedQueries.isEmpty());
    }

    @Test
    public void testExtractCqlExecutionDisabled_commandNotBuffered() {
        CassandraHandler handler = new CassandraHandler();
        handler.setCqlSession(session);
        handler.setExtractCqlExecution(false);

        handler.handle(command("SELECT * FROM " + KEYSPACE + "." + TABLE + " WHERE id = 1"));

        assertTrue(handler.getEvaluatedCqlCommands().isEmpty());
    }

    @Test
    public void testExtractCqlExecutionDisabled_tableSchemaNotRecorded() {
        CassandraHandler handler = new CassandraHandler();
        handler.setCqlSession(session);
        handler.setExtractCqlExecution(false);
        handler.handle(new CassandraTableSchema(TABLE, "{\"id\":\"int\"}"));

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

        CassandraHandler handler = new CassandraHandler();
        handler.setCqlSession(session);

        String matchingCql = "SELECT * FROM " + KEYSPACE + "." + TABLE + " WHERE age = 30";
        String nonMatchingCql = "SELECT * FROM " + KEYSPACE + "." + TABLE + " WHERE age = 18";
        handler.handle(command(matchingCql));
        handler.handle(command(nonMatchingCql));

        List<CqlCommandWithDistance> results = handler.getEvaluatedCqlCommands();

        assertEquals(2, results.size());
        assertEquals(matchingCql, results.get(0).cqlCommand);
        assertEquals(0.0, results.get(0).cqlDistanceWithMetrics.cqlDistance);
        assertEquals(nonMatchingCql, results.get(1).cqlCommand);
        assertTrue(results.get(1).cqlDistanceWithMetrics.cqlDistance > 0.0);
    }

    @Test
    public void testMultipleRows_allRowsFetchedAndBestRowWins() {
        session.execute("INSERT INTO " + KEYSPACE + "." + TABLE + " (id, age, name) VALUES (1, 10, 'A')");
        session.execute("INSERT INTO " + KEYSPACE + "." + TABLE + " (id, age, name) VALUES (2, 30, 'B')");
        session.execute("INSERT INTO " + KEYSPACE + "." + TABLE + " (id, age, name) VALUES (3, 50, 'C')");

        CassandraHandler handler = new CassandraHandler();
        handler.setCqlSession(session);

        handler.handle(command("SELECT * FROM " + KEYSPACE + "." + TABLE + " WHERE age = 30"));
        List<CqlCommandWithDistance> results = handler.getEvaluatedCqlCommands();

        assertEquals(1, results.size());
        assertEquals(0.0, results.get(0).cqlDistanceWithMetrics.cqlDistance);
        assertEquals(3, results.get(0).cqlDistanceWithMetrics.numberOfEvaluatedRows);
    }

    @Test
    public void testCloserMismatchYieldsSmallerDistance() {
        session.execute("INSERT INTO " + KEYSPACE + "." + TABLE + " (id, age, name) VALUES (1, 30, 'John Doe')");

        CassandraHandler closeHandler = new CassandraHandler();
        closeHandler.setCqlSession(session);
        closeHandler.handle(command("SELECT * FROM " + KEYSPACE + "." + TABLE + " WHERE age = 29"));
        double closeDistance = closeHandler.getEvaluatedCqlCommands().get(0).cqlDistanceWithMetrics.cqlDistance;

        CassandraHandler farHandler = new CassandraHandler();
        farHandler.setCqlSession(session);
        farHandler.handle(command("SELECT * FROM " + KEYSPACE + "." + TABLE + " WHERE age = 1000"));
        double farDistance = farHandler.getEvaluatedCqlCommands().get(0).cqlDistanceWithMetrics.cqlDistance;

        assertTrue(closeDistance > 0.0);
        assertTrue(farDistance < 1.0);
        assertTrue(closeDistance < farDistance);
    }

    @Test
    public void testMalformedSelectCommand_returnsMaxValueDistanceWithoutThrowing() {
        CassandraHandler handler = new CassandraHandler();
        handler.setCqlSession(session);

        // starts with SELECT (so it is buffered) but is not valid CQL beyond that
        handler.handle(command("SELECT"));

        List<CqlCommandWithDistance> results = assertDoesNotThrow(handler::getEvaluatedCqlCommands);

        assertEquals(1, results.size());
        assertEquals(Double.MAX_VALUE, results.get(0).cqlDistanceWithMetrics.cqlDistance);
        assertEquals(0, results.get(0).cqlDistanceWithMetrics.numberOfEvaluatedRows);
    }

    @Test
    public void testEmptyTable_noSchemaKnown_tableSchemaIsNull() {
        CassandraHandler handler = new CassandraHandler();
        handler.setCqlSession(session);

        handler.handle(command("SELECT * FROM " + KEYSPACE + "." + TABLE + " WHERE id = 1"));
        handler.getEvaluatedCqlCommands();

        CassandraExecutionsDto dto = handler.getExecutionDto();

        assertEquals(1, dto.failedQueries.size());
        assertNull(dto.failedQueries.get(0).getTableSchema());
    }

    @Test
    public void testNonEmptyTable_notRecordedAsFailedQuery() {
        session.execute("INSERT INTO " + KEYSPACE + "." + TABLE + " (id, age, name) VALUES (1, 30, 'John Doe')");

        CassandraHandler handler = new CassandraHandler();
        handler.setCqlSession(session);
        // no row matches, but the table itself is not empty
        handler.handle(command("SELECT * FROM " + KEYSPACE + "." + TABLE + " WHERE age = 18"));
        handler.getEvaluatedCqlCommands();

        assertTrue(handler.getExecutionDto().failedQueries.isEmpty());
    }

    @Test
    public void testInOperator_endToEnd() {
        session.execute("INSERT INTO " + KEYSPACE + "." + TABLE + " (id, age, name) VALUES (1, 30, 'John Doe')");

        CassandraHandler handler = new CassandraHandler();
        handler.setCqlSession(session);

        String cql = "SELECT * FROM " + KEYSPACE + "." + TABLE + " WHERE age IN (18, 25, 30)";
        handler.handle(command(cql));

        List<CqlCommandWithDistance> results = handler.getEvaluatedCqlCommands();

        assertEquals(1, results.size());
        assertEquals(0.0, results.get(0).cqlDistanceWithMetrics.cqlDistance);
    }

    @Test
    public void testAndOperator_endToEnd() {
        session.execute("INSERT INTO " + KEYSPACE + "." + TABLE + " (id, age, name) VALUES (1, 30, 'John Doe')");

        CassandraHandler handler = new CassandraHandler();
        handler.setCqlSession(session);

        String matchingCql = "SELECT * FROM " + KEYSPACE + "." + TABLE + " WHERE age = 30 AND name = 'John Doe'";
        String partialMismatchCql = "SELECT * FROM " + KEYSPACE + "." + TABLE + " WHERE age = 30 AND name = 'Someone Else'";
        handler.handle(command(matchingCql));
        handler.handle(command(partialMismatchCql));

        List<CqlCommandWithDistance> results = handler.getEvaluatedCqlCommands();

        assertEquals(2, results.size());
        assertEquals(0.0, results.get(0).cqlDistanceWithMetrics.cqlDistance);
        assertTrue(results.get(1).cqlDistanceWithMetrics.cqlDistance > 0.0);
    }
}