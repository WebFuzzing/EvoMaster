package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import com.datastax.oss.driver.api.core.CqlSession;
import org.evomaster.client.java.instrumentation.AdditionalInfo;
import org.evomaster.client.java.instrumentation.CassandraTableSchema;
import org.evomaster.client.java.instrumentation.object.ClassToSchema;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class CassandraTemplateClassReplacementTest {

    private static CqlSession cqlSession;
    private static CassandraTemplate cassandraTemplate;

    private static final int CASSANDRA_PORT = 9042;
    private static final String CASSANDRA_IMAGE = "cassandra";
    private static final String CASSANDRA_VERSION = "4.1";

    private static final GenericContainer<?> cassandra = new GenericContainer<>(CASSANDRA_IMAGE + ":" + CASSANDRA_VERSION)
            .withExposedPorts(CASSANDRA_PORT)
            .waitingFor(Wait.forLogMessage(".*Starting listening for CQL clients.*", 1)
                    .withStartupTimeout(Duration.ofMinutes(2)));

    private static final String KEYSPACE = "testks";
    private static final String TABLE_NAME = "cassandra_template_test_dto";
    private static final String HOST_NAME = "localhost";
    private static final String DATA_CENTER = "datacenter1";

    @BeforeAll
    static void startCassandra() {
        cassandra.start();

        InetSocketAddress contactPoint =
                new InetSocketAddress(HOST_NAME, cassandra.getMappedPort(CASSANDRA_PORT));

        // Bootstrap session, only to create the keyspace (which doesn't exist yet, so we
        // can't bind to it as a default keyspace until after this point)
        try (CqlSession bootstrap = CqlSession.builder()
                .addContactPoint(contactPoint)
                .withLocalDatacenter(DATA_CENTER)
                .build()) {
            bootstrap.execute("CREATE KEYSPACE IF NOT EXISTS " + KEYSPACE +
                    " WITH replication = {'class':'SimpleStrategy','replication_factor':1}");
        }

        // CassandraTemplate's convenience methods (insert/select/selectOneById) build CQL
        // using just the unqualified table name, so the session needs a default keyspace.
        cqlSession = CqlSession.builder()
                .addContactPoint(contactPoint)
                .withLocalDatacenter(DATA_CENTER)
                .withKeyspace(KEYSPACE)
                .build();

        // Setup: call directly on session so it is NOT intercepted by any replacement
        cqlSession.execute("CREATE TABLE IF NOT EXISTS " + TABLE_NAME +
                " (id uuid PRIMARY KEY, name text, age int)");

        cassandraTemplate = new CassandraTemplate(cqlSession);

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
        cqlSession.execute("TRUNCATE " + TABLE_NAME);
        ExecutionTracer.reset();
    }

    private static String expectedSchema() {
        return ClassToSchema.getOrDeriveSchemaWithItsRef(CassandraTemplateTestDto.class, true, Collections.emptyList());
    }

    private static void assertSingleRecordedTableType() {
        List<AdditionalInfo> additionalInfoList = ExecutionTracer.exposeAdditionalInfoList();
        assertEquals(1, additionalInfoList.size());

        Set<CassandraTableSchema> tableTypes = additionalInfoList.get(0).getCassandraTableTypeData();
        assertEquals(1, tableTypes.size());

        CassandraTableSchema tableSchema = tableTypes.iterator().next();
        assertEquals(TABLE_NAME, tableSchema.getTableName());
        assertEquals(expectedSchema(), tableSchema.getTableSchema());
    }

    @Test
    void testInsert() {
        UUID id = UUID.randomUUID();
        CassandraTemplateTestDto dto = new CassandraTemplateTestDto(id, "Alice", 30);

        CassandraTemplateClassReplacement.insert(cassandraTemplate, dto);

        // verify the row was actually persisted, via a direct (untracked) query
        CassandraTemplateTestDto persisted =
                cassandraTemplate.selectOneById(id, CassandraTemplateTestDto.class);
        assertNotNull(persisted);
        assertEquals("Alice", persisted.name);
        assertEquals(30, persisted.age);

        assertSingleRecordedTableType();
    }

    @Test
    void testSelectOne() {
        UUID id = UUID.randomUUID();
        // Direct insert — not intercepted
        cqlSession.execute("INSERT INTO " + TABLE_NAME + " (id, name, age) VALUES (" + id + ", 'Bob', 25)");

        CassandraTemplateTestDto retrieved = CassandraTemplateClassReplacement.selectOne(
                cassandraTemplate, "SELECT * FROM " + TABLE_NAME + " WHERE id = " + id, CassandraTemplateTestDto.class);

        assertNotNull(retrieved);
        assertEquals("Bob", retrieved.name);
        assertEquals(25, retrieved.age);

        assertSingleRecordedTableType();
    }

    @Test
    void testSelect() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        // Direct inserts — not intercepted
        cqlSession.execute("INSERT INTO " + TABLE_NAME + " (id, name, age) VALUES (" + id1 + ", 'Carol', 40)");
        cqlSession.execute("INSERT INTO " + TABLE_NAME + " (id, name, age) VALUES (" + id2 + ", 'Dave', 45)");

        List<CassandraTemplateTestDto> retrieved = CassandraTemplateClassReplacement.select(
                cassandraTemplate, "SELECT * FROM " + TABLE_NAME, CassandraTemplateTestDto.class);

        assertEquals(2, retrieved.size());

        assertSingleRecordedTableType();
    }

    @Test
    void testDirectCallsAreNotTracked() {
        UUID id = UUID.randomUUID();
        CassandraTemplateTestDto dto = new CassandraTemplateTestDto(id, "Erin", 22);

        // Calls that bypass the replacement must not appear in the tracker
        cassandraTemplate.insert(dto);

        List<AdditionalInfo> additionalInfoList = ExecutionTracer.exposeAdditionalInfoList();
        assertEquals(1, additionalInfoList.size());
        assertTrue(additionalInfoList.get(0).getCassandraTableTypeData().isEmpty());
    }
}