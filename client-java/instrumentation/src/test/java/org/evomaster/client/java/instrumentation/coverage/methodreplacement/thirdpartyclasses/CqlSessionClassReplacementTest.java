package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import org.evomaster.client.java.instrumentation.AdditionalInfo;
import org.evomaster.client.java.instrumentation.ExecutedCqlCommand;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class CqlSessionClassReplacementTest {

    private static CqlSession cqlSession;
    private static final int CASSANDRA_PORT = 9042;
    private static final String CASSANDRA_IMAGE = "cassandra";
    private static final String CASSANDRA_VERSION = "4.1";

    private static final GenericContainer<?> cassandra = new GenericContainer<>(CASSANDRA_IMAGE + ":" + CASSANDRA_VERSION)
            .withExposedPorts(CASSANDRA_PORT)
            .waitingFor(Wait.forLogMessage(".*Starting listening for CQL clients.*", 1)
                    .withStartupTimeout(Duration.ofMinutes(2)));

    private static final String KEYSPACE = "testks";
    private static final String TABLE = KEYSPACE + ".users";

    @BeforeAll
    static void startCassandra() {
        cassandra.start();

        cqlSession = CqlSession.builder()
                .addContactPoint(new InetSocketAddress("localhost", cassandra.getMappedPort(CASSANDRA_PORT)))
                .withLocalDatacenter("datacenter1")
                .build();

        // Setup: call directly on session so it is NOT intercepted by the replacement
        cqlSession.execute("CREATE KEYSPACE IF NOT EXISTS " + KEYSPACE +
                " WITH replication = {'class':'SimpleStrategy','replication_factor':1}");
        cqlSession.execute("CREATE TABLE IF NOT EXISTS " + TABLE +
                " (id uuid PRIMARY KEY, name text, age int)");

        ExecutionTracer.reset();
    }

    @AfterAll
    static void cleanup() {
        if (cqlSession != null) {
            cqlSession.close();
        }
        ExecutionTracer.reset();
    }

    @BeforeEach
    void clearTable() {
        // Direct call — not intercepted
        cqlSession.execute("TRUNCATE " + TABLE);
        ExecutionTracer.reset();
    }

    @Test
    void testExecuteSelectIsTracked() {
        String query = "SELECT * FROM " + TABLE;

        CqlSessionClassReplacement.execute(cqlSession, query);

        List<AdditionalInfo> additionalInfoList = ExecutionTracer.exposeAdditionalInfoList();
        assertEquals(1, additionalInfoList.size());

        Set<ExecutedCqlCommand> commands = additionalInfoList.get(0).getCqlInfoData();
        assertEquals(1, commands.size());

        ExecutedCqlCommand cmd = commands.iterator().next();
        assertEquals(query, cmd.getCqlCommand());
        assertFalse(cmd.hasThrownCqlException());
        assertTrue(cmd.getExecutionTime() >= 0);
    }

    @Test
    void testExecuteInsertIsTracked() {
        String query = "INSERT INTO " + TABLE + " (id, name, age) VALUES (uuid(), 'Alice', 30)";

        CqlSessionClassReplacement.execute(cqlSession, query);

        List<AdditionalInfo> additionalInfoList = ExecutionTracer.exposeAdditionalInfoList();
        assertEquals(1, additionalInfoList.size());

        Set<ExecutedCqlCommand> commands = additionalInfoList.get(0).getCqlInfoData();
        assertEquals(1, commands.size());

        ExecutedCqlCommand cmd = commands.iterator().next();
        assertEquals(query, cmd.getCqlCommand());
        assertFalse(cmd.hasThrownCqlException());
        assertTrue(cmd.getExecutionTime() >= 0);
    }

    @Test
    void testMultipleExecutionsAreAllTracked() {
        String insert = "INSERT INTO " + TABLE + " (id, name, age) VALUES (uuid(), 'Bob', 25)";
        String select = "SELECT * FROM " + TABLE;

        CqlSessionClassReplacement.execute(cqlSession, insert);
        CqlSessionClassReplacement.execute(cqlSession, select);

        List<AdditionalInfo> additionalInfoList = ExecutionTracer.exposeAdditionalInfoList();
        assertEquals(1, additionalInfoList.size());

        Set<ExecutedCqlCommand> commands = additionalInfoList.get(0).getCqlInfoData();
        assertEquals(2, commands.size());
    }

    @Test
    void testDirectCallsAreNotTracked() {
        // Calls that bypass the replacement must not appear in the tracker
        cqlSession.execute("INSERT INTO " + TABLE + " (id, name, age) VALUES (uuid(), 'Carol', 20)");

        List<AdditionalInfo> additionalInfoList = ExecutionTracer.exposeAdditionalInfoList();
        assertEquals(1, additionalInfoList.size());
        assertTrue(additionalInfoList.get(0).getCqlInfoData().isEmpty());
    }

    @Test
    void testExecutingInitCassandraFlagSuppressesTracking() {
        ExecutionTracer.setExecutingInitCassandra(true);
        try {
            CqlSessionClassReplacement.execute(cqlSession, "SELECT * FROM " + TABLE);
        } finally {
            ExecutionTracer.setExecutingInitCassandra(false);
        }

        List<AdditionalInfo> additionalInfoList = ExecutionTracer.exposeAdditionalInfoList();
        assertEquals(1, additionalInfoList.size());
        assertTrue(additionalInfoList.get(0).getCqlInfoData().isEmpty());
    }

    @Test
    void testExecuteWithPositionalValuesIsTracked() {
        String query = "INSERT INTO " + TABLE + " (id, name, age) VALUES (?, ?, ?)";

        CqlSessionClassReplacement.execute(cqlSession, query, UUID.randomUUID(), "Dave", 40);

        List<AdditionalInfo> additionalInfoList = ExecutionTracer.exposeAdditionalInfoList();
        assertEquals(1, additionalInfoList.size());

        Set<ExecutedCqlCommand> commands = additionalInfoList.get(0).getCqlInfoData();
        assertEquals(1, commands.size());

        ExecutedCqlCommand cmd = commands.iterator().next();
        assertEquals(query, cmd.getCqlCommand());
        assertFalse(cmd.hasThrownCqlException());
        assertTrue(cmd.getExecutionTime() >= 0);
    }

    @Test
    void testExecuteWithNamedValuesIsTracked() {
        String query = "INSERT INTO " + TABLE + " (id, name, age) VALUES (:id, :name, :age)";
        Map<String, Object> values = new HashMap<>();
        values.put("id", UUID.randomUUID());
        values.put("name", "Erin");
        values.put("age", 22);

        CqlSessionClassReplacement.execute(cqlSession, query, values);

        List<AdditionalInfo> additionalInfoList = ExecutionTracer.exposeAdditionalInfoList();
        assertEquals(1, additionalInfoList.size());

        Set<ExecutedCqlCommand> commands = additionalInfoList.get(0).getCqlInfoData();
        assertEquals(1, commands.size());

        ExecutedCqlCommand cmd = commands.iterator().next();
        assertEquals(query, cmd.getCqlCommand());
        assertFalse(cmd.hasThrownCqlException());
        assertTrue(cmd.getExecutionTime() >= 0);
    }

    @Test
    void testExecuteWithSimpleStatementIsTracked() {
        String query = "SELECT * FROM " + TABLE;
        SimpleStatement statement = SimpleStatement.newInstance(query);

        CqlSessionClassReplacement.execute(cqlSession, statement);

        List<AdditionalInfo> additionalInfoList = ExecutionTracer.exposeAdditionalInfoList();
        assertEquals(1, additionalInfoList.size());

        Set<ExecutedCqlCommand> commands = additionalInfoList.get(0).getCqlInfoData();
        assertEquals(1, commands.size());

        ExecutedCqlCommand cmd = commands.iterator().next();
        assertEquals(query, cmd.getCqlCommand());
        assertFalse(cmd.hasThrownCqlException());
        assertTrue(cmd.getExecutionTime() >= 0);
    }

    @Test
    void testExecuteWithBoundStatementIsTracked() {
        String query = "INSERT INTO " + TABLE + " (id, name, age) VALUES (?, ?, ?)";
        // Preparing directly on the session so it is NOT intercepted by the replacement
        PreparedStatement prepared = cqlSession.prepare(query);
        BoundStatement bound = prepared.bind(UUID.randomUUID(), "Frank", 33);

        CqlSessionClassReplacement.execute(cqlSession, bound);

        List<AdditionalInfo> additionalInfoList = ExecutionTracer.exposeAdditionalInfoList();
        assertEquals(1, additionalInfoList.size());

        Set<ExecutedCqlCommand> commands = additionalInfoList.get(0).getCqlInfoData();
        assertEquals(1, commands.size());

        ExecutedCqlCommand cmd = commands.iterator().next();
        assertEquals(query, cmd.getCqlCommand());
        assertFalse(cmd.hasThrownCqlException());
        assertTrue(cmd.getExecutionTime() >= 0);
    }
}
