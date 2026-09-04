package org.evomaster.client.java.controller.cassandra.insertions;

import com.datastax.oss.driver.api.core.CqlSession;
import org.evomaster.client.java.controller.api.dto.database.operations.CassandraInsertionDto;
import org.evomaster.client.java.controller.api.dto.database.operations.CassandraInsertionResultsDto;
import org.evomaster.client.java.controller.cassandra.dsl.CassandraDsl;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
                " (id int PRIMARY KEY, name text, elapsed duration)");
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

    /**
     * A duration is written as a bare literal, ie not enclosed in quotes the way a text is, and it
     * carries at most one leading sign, applying to the whole value. This is what
     * CassandraLiteralRenderer in the core module relies on when rendering the value of a
     * CqlDurationGene, so it is checked here against a real Cassandra.
     */
    @Test
    public void testInsertDuration() {
        List<CassandraInsertionDto> insertions = CassandraDsl.cassandra()
                .insertInto(KEYSPACE, TABLE).d("id", "1").d("elapsed", "1mo2d3ns")
                .and().insertInto(KEYSPACE, TABLE).d("id", "2").d("elapsed", "-1mo2d3ns")
                .dtos();

        CassandraInsertionResultsDto resultsDto = CassandraScriptRunner.executeInsert(connection, insertions);

        assertTrue(resultsDto.executionResults.get(0));
        assertTrue(resultsDto.executionResults.get(1));
        assertEquals(2, connection.execute("SELECT * FROM " + KEYSPACE + "." + TABLE).all().size());
    }

    @Test
    public void testInsertionFailureDoesNotStopFollowingInsertions() {

        /*
            The second insertion fails, as a quoted string is given for the "id" column, which is an int.
            Note that it could not fail by reusing an existing id, as in Cassandra that would be an upsert.
         */
        List<CassandraInsertionDto> insertions = CassandraDsl.cassandra()
                .insertInto(KEYSPACE, TABLE).d("id", "1").d("name", "'first'")
                .and().insertInto(KEYSPACE, TABLE).d("id", "'notAnInt'").d("name", "'broken'")
                .and().insertInto(KEYSPACE, TABLE).d("id", "3").d("name", "'third'")
                .dtos();

        CassandraInsertionResultsDto resultsDto = CassandraScriptRunner.executeInsert(connection, insertions);

        assertTrue(resultsDto.executionResults.get(0));
        assertFalse(resultsDto.executionResults.get(1));
        assertTrue(resultsDto.executionResults.get(2));

        //the third insertion must have been attempted, in spite of the second one having failed
        assertEquals(2, connection.execute("SELECT * FROM " + KEYSPACE + "." + TABLE).all().size());
    }
}
