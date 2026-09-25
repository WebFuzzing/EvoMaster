package org.evomaster.client.java.controller.cassandra;

import com.datastax.oss.driver.api.core.CqlSession;
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
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CassandraCleanerTest {

    private static CqlSession connection;
    private static final int CASSANDRA_PORT = 9042;
    private static final String CASSANDRA_IMAGE = "cassandra";
    private static final String CASSANDRA_VERSION = "4.1";

    private static final GenericContainer<?> cassandra = new GenericContainer<>(CASSANDRA_IMAGE + ":" + CASSANDRA_VERSION)
            .withExposedPorts(CASSANDRA_PORT)
            .waitingFor(Wait.forLogMessage(".*Starting listening for CQL clients.*", 1)
                    .withStartupTimeout(Duration.ofMinutes(2)));

    private static final String KEYSPACE = "cleanerks";
    private static final String OTHER_KEYSPACE = "otherks";

    @BeforeAll
    public static void initClass() {
        cassandra.start();

        connection = CqlSession.builder()
                .addContactPoint(new InetSocketAddress("localhost", cassandra.getMappedPort(CASSANDRA_PORT)))
                .withLocalDatacenter("datacenter1")
                .build();

        createKeyspace(KEYSPACE);
        createKeyspace(OTHER_KEYSPACE);

        connection.execute("CREATE TABLE IF NOT EXISTS " + KEYSPACE + ".users (id int PRIMARY KEY, name text)");
        connection.execute("CREATE TABLE IF NOT EXISTS " + KEYSPACE + ".events (id int PRIMARY KEY, label text)");
        // a mixed-case name only survives in the schema if quoted, and so must be quoted to be truncated
        connection.execute("CREATE TABLE IF NOT EXISTS " + KEYSPACE + ".\"MixedCase\" (id int PRIMARY KEY)");
        connection.execute("CREATE TABLE IF NOT EXISTS " + OTHER_KEYSPACE + ".users (id int PRIMARY KEY)");
    }

    private static void createKeyspace(String name) {
        connection.execute("CREATE KEYSPACE IF NOT EXISTS " + name +
                " WITH replication = {'class':'SimpleStrategy','replication_factor':1}");
    }

    @AfterAll
    public static void cleanup() {
        if (connection != null) {
            connection.close();
        }
    }

    @BeforeEach
    public void insertOneRowPerTable() {
        ExecutionTracer.setExecutingInitCassandra(false);

        connection.execute("INSERT INTO " + KEYSPACE + ".users (id, name) VALUES (1, 'foo')");
        connection.execute("INSERT INTO " + KEYSPACE + ".events (id, label) VALUES (1, 'bar')");
        connection.execute("INSERT INTO " + KEYSPACE + ".\"MixedCase\" (id) VALUES (1)");
        connection.execute("INSERT INTO " + OTHER_KEYSPACE + ".users (id) VALUES (1)");
    }

    private boolean hasRows(String keyspace, String table) {
        return connection.execute("SELECT * FROM " + keyspace + "." + table).iterator().hasNext();
    }

    @Test
    public void testAllTablesOfTheKeyspaceAreCleared() {
        assertTrue(hasRows(KEYSPACE, "users"));
        assertTrue(hasRows(KEYSPACE, "events"));

        CassandraCleaner.clearKeyspaces(connection, Collections.singletonList(KEYSPACE));

        assertFalse(hasRows(KEYSPACE, "users"));
        assertFalse(hasRows(KEYSPACE, "events"));
    }

    /**
     * The tables are truncated, not dropped: the SUT creates its schema once at start-up, so losing
     * it would break every test after the first.
     */
    @Test
    public void testTablesStillExistAfterClearing() {
        CassandraCleaner.clearKeyspaces(connection, Collections.singletonList(KEYSPACE));

        assertFalse(hasRows(KEYSPACE, "users"));
        assertFalse(hasRows(KEYSPACE, "events"));
        assertFalse(hasRows(KEYSPACE, "\"MixedCase\""));
    }

    @Test
    public void testMixedCaseTableIsCleared() {
        assertTrue(hasRows(KEYSPACE, "\"MixedCase\""));

        CassandraCleaner.clearKeyspaces(connection, Collections.singletonList(KEYSPACE));

        assertFalse(hasRows(KEYSPACE, "\"MixedCase\""));
    }

    @Test
    public void testOtherKeyspacesAreUntouched() {
        CassandraCleaner.clearKeyspaces(connection, Collections.singletonList(KEYSPACE));

        assertTrue(hasRows(OTHER_KEYSPACE, "users"));
    }

    @Test
    public void testSeveralKeyspacesAreCleared() {
        CassandraCleaner.clearKeyspaces(connection, java.util.Arrays.asList(KEYSPACE, OTHER_KEYSPACE));

        assertFalse(hasRows(KEYSPACE, "users"));
        assertFalse(hasRows(OTHER_KEYSPACE, "users"));
    }

    @Test
    public void testNoKeyspaceIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> CassandraCleaner.clearKeyspaces(connection, Collections.emptyList()));
        assertThrows(IllegalArgumentException.class,
                () -> CassandraCleaner.clearKeyspaces(connection, null));
    }

    @Test
    public void testNoSessionIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> CassandraCleaner.clearKeyspaces(null, Collections.singletonList(KEYSPACE)));
    }

    @Test
    public void testUnknownKeyspaceIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> CassandraCleaner.clearKeyspaces(connection, Collections.singletonList("nosuchks")));
    }

    @Test
    public void testSystemKeyspaceIsRejected() {
        List<String> system = Collections.singletonList("system_schema");

        assertThrows(IllegalArgumentException.class,
                () -> CassandraCleaner.clearKeyspaces(connection, system));

        //the data of the keyspace asked for before it must be left alone as well
        assertThrows(IllegalArgumentException.class,
                () -> CassandraCleaner.clearKeyspaces(connection, java.util.Arrays.asList(KEYSPACE, "system")));
        assertTrue(hasRows(KEYSPACE, "users"));
    }

    /**
     * The clean-up is not SUT traffic, so the tracing must be back on once it returns, including when
     * it throws. There is no getter for the flag, so it is checked through what it gates: a CQL
     * command is only recorded while the flag is off.
     */
    @Test
    public void testTracingIsRestored() {
        CassandraCleaner.clearKeyspaces(connection, Collections.singletonList(KEYSPACE));
        assertTrue(isCqlCommandRecorded());

        assertThrows(IllegalArgumentException.class,
                () -> CassandraCleaner.clearKeyspaces(connection, Collections.singletonList("nosuchks")));
        assertTrue(isCqlCommandRecorded());
    }

    private boolean isCqlCommandRecorded() {
        ExecutionTracer.reset();
        ExecutionTracer.addCqlInfo(new ExecutedCqlCommand(
                "SELECT * FROM " + KEYSPACE + ".users", KEYSPACE, "users", false, 0L));

        return ExecutionTracer.exposeAdditionalInfoList().stream()
                .anyMatch(info -> !info.getCqlInfoData().isEmpty());
    }
}