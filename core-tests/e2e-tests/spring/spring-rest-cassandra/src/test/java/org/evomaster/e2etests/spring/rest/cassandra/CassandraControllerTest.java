package org.evomaster.e2etests.spring.rest.cassandra;

import com.cassandra.findbyage.CassandraFindByAgeApp;
import com.cassandra.findbydayrange.CassandraFindByDayRangeApp;
import com.cassandra.findbytag.CassandraFindByTagApp;
import com.cassandra.findbyuuid.CassandraFindByUuidApp;
import com.datastax.oss.driver.api.core.CqlIdentifier;
import com.datastax.oss.driver.api.core.CqlSession;
import com.foo.spring.rest.cassandra.CassandraController;
import com.foo.spring.rest.cassandra.findbyage.CassandraFindByAgeController;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class CassandraControllerTest {

    private static final int CASSANDRA_PORT = 9042;
    private static final String KEYSPACE = "evomaster_e2e";

    /**
     * One container for every app of the module: starting it is what dominates the time of this
     * suite, so the apps share it instead of each starting its own, as they would through their
     * controller.
     */
    private static final GenericContainer<?> cassandra = new GenericContainer<>("cassandra:4.1")
            .withExposedPorts(CASSANDRA_PORT)
            .waitingFor(Wait.forLogMessage(".*Starting listening for CQL clients.*", 1)
                    .withStartupTimeout(Duration.ofMinutes(3)));

    @BeforeAll
    public static void initClass() {
        cassandra.start();
    }

    @AfterAll
    public static void tearDown() {
        cassandra.stop();
    }

    /**
     * The controllers are thin subclasses of {@link CassandraController}, so one of them starting is
     * enough to cover starting the container, running the app and clearing the keyspace.
     */
    @Test
    public void testCanStartSut() {
        CassandraController controller = new CassandraFindByAgeController();
        controller.startSut();
        assertTrue(controller.isSutRunning());
        controller.stopSut();
    }

    /**
     * Each app creates the schema of its own scenario at start-up, so this is what says the DDL of
     * each is valid and that its table is where the SUT, and later the generated insertions, expect
     * it to be.
     */
    @ParameterizedTest
    @CsvSource({
            "com.cassandra.findbyage.CassandraFindByAgeApp, person_by_age",
            "com.cassandra.findbyuuid.CassandraFindByUuidApp, record_by_id",
            "com.cassandra.findbydayrange.CassandraFindByDayRangeApp, measurement_by_day",
            "com.cassandra.findbytag.CassandraFindByTagApp, session_by_id"
    })
    public void testAppCreatesItsTable(String appClassName, String tableName) throws Exception {

        Class<?> appClass = Class.forName(appClassName);

        SpringApplicationBuilder app = new SpringApplicationBuilder(appClass);
        app.properties(
                "--server.port=0",
                "cassandra.host=" + cassandra.getHost(),
                "cassandra.port=" + cassandra.getMappedPort(CASSANDRA_PORT)
        );

        try (ConfigurableApplicationContext ctx = app.run()) {
            CqlSession session = ctx.getBean(CqlSession.class);

            assertTrue(session.getMetadata()
                    .getKeyspace(CqlIdentifier.fromCql(KEYSPACE))
                    .flatMap(k -> k.getTable(CqlIdentifier.fromCql(tableName)))
                    .isPresent());
        }
    }

    /**
     * Only referenced so that a renamed app class breaks this test at compile time, instead of at run
     * time through the names above.
     */
    @SuppressWarnings("unused")
    private static final Class<?>[] APPS = {
            CassandraFindByAgeApp.class,
            CassandraFindByUuidApp.class,
            CassandraFindByDayRangeApp.class,
            CassandraFindByTagApp.class
    };
}