package org.evomaster.client.java.controller.cassandra.insertions;

import com.datastax.oss.driver.api.core.CqlSession;
import org.evomaster.client.java.controller.cassandra.dsl.CassandraDsl;
import org.evomaster.client.java.controller.cassandra.insertions.model.CassandraInsertionDto;
import org.evomaster.client.java.controller.cassandra.insertions.model.CassandraInsertionResultsDto;
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

public class CassandraScriptRunnerTest {

    private static CqlSession connection;
    private static final int CASSANDRA_PORT = 9042;
    private static final String CASSANDRA_IMAGE = "cassandra";
    private static final String CASSANDRA_VERSION = "4.1";

    private static final GenericContainer<?> cassandra = new GenericContainer<>(CASSANDRA_IMAGE + ":" + CASSANDRA_VERSION)
            .withExposedPorts(CASSANDRA_PORT)
            .waitingFor(Wait.forLogMessage(".*Starting listening for CQL clients.*", 1)
                    .withStartupTimeout(Duration.ofMinutes(2)));

    private static final String KEYSPACE = "testks";
    private static final String TABLE = "users";

    @BeforeAll
    public static void initClass() {
        cassandra.start();

        connection = CqlSession.builder()
                .addContactPoint(new InetSocketAddress("localhost", cassandra.getMappedPort(CASSANDRA_PORT)))
                .withLocalDatacenter("datacenter1")
                .build();

        connection.execute("CREATE KEYSPACE IF NOT EXISTS " + KEYSPACE +
                " WITH replication = {'class':'SimpleStrategy','replication_factor':1}");
        connection.execute("CREATE TABLE IF NOT EXISTS " + KEYSPACE + "." + TABLE +
                " (id int PRIMARY KEY, name text)");
    }

    @AfterAll
    public static void cleanup() {
        if (connection != null) {
            connection.close();
        }
    }

    @BeforeEach
    public void clearTable() {
        connection.execute("TRUNCATE " + KEYSPACE + "." + TABLE);
    }

    @Test
    public void testInsert() {
        assertFalse(connection.execute("SELECT * FROM " + KEYSPACE + "." + TABLE).iterator().hasNext());

        List<CassandraInsertionDto> insertions = CassandraDsl.cassandra()
                .insertInto(KEYSPACE, TABLE)
                .d("id", "1")
                .d("name", "'aName'")
                .dtos();

        CassandraInsertionResultsDto resultsDto = CassandraScriptRunner.executeInsert(connection, insertions);

        assertTrue(resultsDto.executionResults.get(0));
        assertTrue(connection.execute("SELECT * FROM " + KEYSPACE + "." + TABLE).iterator().hasNext());
    }
}
