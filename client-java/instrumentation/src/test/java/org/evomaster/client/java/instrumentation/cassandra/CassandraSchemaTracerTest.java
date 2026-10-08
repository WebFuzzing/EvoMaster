package org.evomaster.client.java.instrumentation.cassandra;

import com.datastax.oss.driver.api.core.CqlSession;
import org.evomaster.client.java.instrumentation.AdditionalInfo;
import org.evomaster.client.java.instrumentation.InstrumentationController;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class CassandraSchemaTracerTest {

    private static CqlSession cqlSession;

    private static final int CASSANDRA_PORT = 9042;
    private static final String CASSANDRA_IMAGE = "cassandra";
    private static final String CASSANDRA_VERSION = "4.1";

    private static final GenericContainer<?> cassandra = new GenericContainer<>(CASSANDRA_IMAGE + ":" + CASSANDRA_VERSION)
            .withExposedPorts(CASSANDRA_PORT)
            .waitingFor(Wait.forLogMessage(".*Starting listening for CQL clients.*", 1)
                    .withStartupTimeout(Duration.ofMinutes(2)));

    private static final String HOST_NAME = "localhost";
    private static final String DATA_CENTER = "datacenter1";

    private static final String KEYSPACE_1 = "schema_repo_ks1";
    private static final String KEYSPACE_2 = "schema_repo_ks2";

    @BeforeAll
    static void startCassandra() {
        cassandra.start();

        InetSocketAddress contactPoint =
                new InetSocketAddress(HOST_NAME, cassandra.getMappedPort(CASSANDRA_PORT));

        cqlSession = CqlSession.builder()
                .addContactPoint(contactPoint)
                .withLocalDatacenter(DATA_CENTER)
                .build();

        cqlSession.execute("CREATE KEYSPACE IF NOT EXISTS " + KEYSPACE_1 +
                " WITH replication = {'class':'SimpleStrategy','replication_factor':1}");
        cqlSession.execute("CREATE KEYSPACE IF NOT EXISTS " + KEYSPACE_2 +
                " WITH replication = {'class':'SimpleStrategy','replication_factor':1}");

        // simple single-column primary key
        cqlSession.execute("CREATE TABLE IF NOT EXISTS " + KEYSPACE_1 + ".table_a" +
                " (id uuid PRIMARY KEY, name text)");

        // composite partition key + clustering column, to exercise PK/clustering tagging
        cqlSession.execute("CREATE TABLE IF NOT EXISTS " + KEYSPACE_2 + ".table_b" +
                " (part1 int, part2 int, rank int, payload text, PRIMARY KEY((part1, part2), rank))");

        // a quoted, mixed-case keyspace/table with a different shape than any lowercase twin
        cqlSession.execute("CREATE KEYSPACE IF NOT EXISTS \"" + "SchemaRepoMixedCaseKs" + "\"" +
                " WITH replication = {'class':'SimpleStrategy','replication_factor':1}");
        cqlSession.execute("CREATE TABLE IF NOT EXISTS \"SchemaRepoMixedCaseKs\".\"MixedCaseTable\"" +
                " (id int PRIMARY KEY, amount int)");
        // a lowercase twin, with a deliberately different column set
        cqlSession.execute("CREATE TABLE IF NOT EXISTS " + KEYSPACE_1 + ".mixedcasetable" +
                " (id int PRIMARY KEY, unrelated_column text)");
    }

    @AfterAll
    static void cleanup() {
        if (cqlSession != null) {
            cqlSession.close();
        }
        ExecutionTracer.reset();
    }

    @BeforeEach
    void resetTracer() {
        ExecutionTracer.reset();
    }

    private static CassandraColumnMetadata columnNamed(CassandraTableMetadata table, String name) {
        return table.getColumns().stream()
                .filter(c -> c.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No column named " + name));
    }

    @Test
    void resolve_capturesTablesAcrossMultipleKeyspaces() {
        CassandraTableMetadata tableA = CassandraSchemaTracer.resolve(cqlSession, KEYSPACE_1, "table_a");
        CassandraTableMetadata tableB = CassandraSchemaTracer.resolve(cqlSession, KEYSPACE_2, "table_b");

        assertNotNull(tableA);
        assertEquals(KEYSPACE_1, tableA.getKeyspaceName());
        assertEquals("table_a", tableA.getTableName());

        assertNotNull(tableB);
        assertEquals(KEYSPACE_2, tableB.getKeyspaceName());
        assertEquals("table_b", tableB.getTableName());
    }

    @Test
    void resolve_taggedPartitionKeyAndClusteringColumns() {
        CassandraTableMetadata tableB = CassandraSchemaTracer.resolve(cqlSession, KEYSPACE_2, "table_b");
        assertNotNull(tableB);

        assertTrue(columnNamed(tableB, "part1").isPartitionKey());
        assertTrue(columnNamed(tableB, "part2").isPartitionKey());
        assertFalse(columnNamed(tableB, "part1").isClusteringColumn());

        assertTrue(columnNamed(tableB, "rank").isClusteringColumn());
        assertFalse(columnNamed(tableB, "rank").isPartitionKey());

        CassandraColumnMetadata payload = columnNamed(tableB, "payload");
        assertFalse(payload.isPartitionKey());
        assertFalse(payload.isClusteringColumn());
        assertEquals("text", payload.getCqlType());
    }

    @Test
    void resolve_unseenTable_capturedOnDemandWithoutError() {
        cqlSession.execute("CREATE TABLE IF NOT EXISTS " + KEYSPACE_1 + ".table_created_after_snapshot" +
                " (id uuid PRIMARY KEY, age int)");

        CassandraTableMetadata table = CassandraSchemaTracer.resolve(cqlSession, KEYSPACE_1, "table_created_after_snapshot");

        assertNotNull(table);
        assertEquals("table_created_after_snapshot", table.getTableName());
        assertTrue(columnNamed(table, "id").isPartitionKey());
        assertEquals("int", columnNamed(table, "age").getCqlType());
    }

    @Test
    void resolve_unqualifiedReference_usesSessionCurrentKeyspace() {
        InetSocketAddress contactPoint =
                new InetSocketAddress(HOST_NAME, cassandra.getMappedPort(CASSANDRA_PORT));
        try (CqlSession sessionWithDefaultKeyspace = CqlSession.builder()
                .addContactPoint(contactPoint)
                .withLocalDatacenter(DATA_CENTER)
                .withKeyspace(KEYSPACE_1)
                .build()) {

            CassandraTableMetadata table = CassandraSchemaTracer.resolve(sessionWithDefaultKeyspace, null, "table_a");

            assertNotNull(table);
            assertEquals(KEYSPACE_1, table.getKeyspaceName());
            assertEquals("table_a", table.getTableName());
        }
    }

    @Test
    void resolve_unqualifiedReference_noCurrentKeyspace_returnsNull() {
        assertFalse(cqlSession.getKeyspace().isPresent());

        CassandraTableMetadata table = CassandraSchemaTracer.resolve(cqlSession, null, "table_a");

        assertNull(table);
    }

    @Test
    void resolve_nonexistentTable_returnsNull() {
        CassandraTableMetadata table = CassandraSchemaTracer.resolve(cqlSession, KEYSPACE_1, "no_such_table");

        assertNull(table);
    }

    @Test
    void resolve_nonexistentKeyspace_returnsNull() {
        CassandraTableMetadata table = CassandraSchemaTracer.resolve(cqlSession, "no_such_keyspace", "table_a");

        assertNull(table);
    }

    @Test
    void resolveKeyspaceName_explicitQualifier_returnsItsCanonicalForm() {
        String keyspaceName = CassandraSchemaTracer.resolveKeyspaceName(cqlSession, KEYSPACE_1);

        assertEquals(KEYSPACE_1, keyspaceName);
    }

    @Test
    void resolveKeyspaceName_unqualified_usesSessionCurrentKeyspace() {
        InetSocketAddress contactPoint =
                new InetSocketAddress(HOST_NAME, cassandra.getMappedPort(CASSANDRA_PORT));
        try (CqlSession sessionWithDefaultKeyspace = CqlSession.builder()
                .addContactPoint(contactPoint)
                .withLocalDatacenter(DATA_CENTER)
                .withKeyspace(KEYSPACE_1)
                .build()) {

            String keyspaceName = CassandraSchemaTracer.resolveKeyspaceName(sessionWithDefaultKeyspace, null);

            assertEquals(KEYSPACE_1, keyspaceName);
        }
    }

    @Test
    void resolveKeyspaceName_unqualified_noCurrentKeyspace_returnsNull() {
        assertFalse(cqlSession.getKeyspace().isPresent());

        String keyspaceName = CassandraSchemaTracer.resolveKeyspaceName(cqlSession, null);

        assertNull(keyspaceName);
    }

    @Test
    void resolveKeyspaceName_nonexistentKeyspace_returnsNull() {
        String keyspaceName = CassandraSchemaTracer.resolveKeyspaceName(cqlSession, "no_such_keyspace");

        assertNull(keyspaceName);
    }

    @Test
    void resolve_quotedMixedCaseKeyspaceAndTable_targetsCorrectOne() {
        CassandraTableMetadata mixedCase = CassandraSchemaTracer.resolve(
                cqlSession, "\"SchemaRepoMixedCaseKs\"", "\"MixedCaseTable\"");

        assertNotNull(mixedCase);
        assertEquals("SchemaRepoMixedCaseKs", mixedCase.getKeyspaceName());
        assertEquals("MixedCaseTable", mixedCase.getTableName());
        assertNotNull(columnNamed(mixedCase, "amount"));

        // the lowercase, unquoted twin in a different keyspace must not be conflated with it
        CassandraTableMetadata lowercaseTwin = CassandraSchemaTracer.resolve(cqlSession, KEYSPACE_1, "mixedcasetable");
        assertNotNull(lowercaseTwin);
        assertNotNull(columnNamed(lowercaseTwin, "unrelated_column"));
    }

    @Test
    void resolve_calledTwiceForSameTable_returnsEqualMetadata() {
        CassandraTableMetadata first = CassandraSchemaTracer.resolve(cqlSession, KEYSPACE_1, "table_a");
        CassandraTableMetadata second = CassandraSchemaTracer.resolve(cqlSession, KEYSPACE_1, "table_a");

        assertNotNull(first);
        assertEquals(first, second);
    }

    @Test
    void resolve_firstTimeSeen_tracesSchemaViaExecutionTracer() {
        cqlSession.execute("CREATE TABLE IF NOT EXISTS " + KEYSPACE_1 + ".trace_once_table" +
                " (id uuid PRIMARY KEY, age int)");

        CassandraTableMetadata table = CassandraSchemaTracer.resolve(cqlSession, KEYSPACE_1, "trace_once_table");
        assertNotNull(table);

        List<AdditionalInfo> additionalInfoList = ExecutionTracer.exposeAdditionalInfoList();
        assertEquals(1, additionalInfoList.size());

        Set<CassandraTableMetadata> traced = additionalInfoList.get(0).getCassandraTableMetadataData();
        assertEquals(1, traced.size());
        assertEquals(table, traced.iterator().next());
    }

    @Test
    void resolve_alreadyCached_doesNotRetraceSchema() {
        cqlSession.execute("CREATE TABLE IF NOT EXISTS " + KEYSPACE_1 + ".trace_no_duplicate_table" +
                " (id uuid PRIMARY KEY, age int)");

        // first call: not yet cached, traces the schema
        CassandraSchemaTracer.resolve(cqlSession, KEYSPACE_1, "trace_no_duplicate_table");
        ExecutionTracer.reset();

        // second call: already cached, must not trace again
        CassandraSchemaTracer.resolve(cqlSession, KEYSPACE_1, "trace_no_duplicate_table");

        List<AdditionalInfo> additionalInfoList = ExecutionTracer.exposeAdditionalInfoList();
        assertEquals(1, additionalInfoList.size());
        assertTrue(additionalInfoList.get(0).getCassandraTableMetadataData().isEmpty());
    }

    @Test
    void resolve_afterResetForNewSearch_retracesSchema() {
        cqlSession.execute("CREATE TABLE IF NOT EXISTS " + KEYSPACE_1 + ".reset_new_search_table" +
                " (id uuid PRIMARY KEY, age int)");

        CassandraSchemaTracer.resolve(cqlSession, KEYSPACE_1, "reset_new_search_table");
        ExecutionTracer.reset();

        InstrumentationController.resetForNewSearch();
        CassandraSchemaTracer.resolve(cqlSession, KEYSPACE_1, "reset_new_search_table");

        List<AdditionalInfo> additionalInfoList = ExecutionTracer.exposeAdditionalInfoList();
        assertEquals(1, additionalInfoList.size());
        // cache was cleared by resetForNewSearch(), so the second resolve() must re-trace
        assertEquals(1, additionalInfoList.get(0).getCassandraTableMetadataData().size());
    }

    @Test
    void resolve_afterResetForNewTest_doesNotRetraceSchema() {
        cqlSession.execute("CREATE TABLE IF NOT EXISTS " + KEYSPACE_1 + ".reset_new_test_table" +
                " (id uuid PRIMARY KEY, age int)");

        CassandraSchemaTracer.resolve(cqlSession, KEYSPACE_1, "reset_new_test_table");
        ExecutionTracer.reset();

        InstrumentationController.resetForNewTest();
        CassandraSchemaTracer.resolve(cqlSession, KEYSPACE_1, "reset_new_test_table");

        List<AdditionalInfo> additionalInfoList = ExecutionTracer.exposeAdditionalInfoList();
        assertEquals(1, additionalInfoList.size());
        // per-test reset must NOT clear the schema cache
        assertTrue(additionalInfoList.get(0).getCassandraTableMetadataData().isEmpty());
    }
}