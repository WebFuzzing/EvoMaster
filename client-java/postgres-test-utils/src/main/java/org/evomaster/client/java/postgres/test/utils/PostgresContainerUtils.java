package org.evomaster.client.java.postgres.test.utils;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.util.Collections;

/**
 * Shared utility for creating and configuring PostgreSQL Testcontainers instances.
 * Used across modules (client-java/sql, core, e2e tests) to avoid code duplication.
 */
public class PostgresContainerUtils {

    /**
     * Default version of PostgreSQL used when none is explicitly specified.
     */
    public static final String DEFAULT_POSTGRES_VERSION = "14";

    /**
     * Creates a new PostgreSQL container using the default version ({@value DEFAULT_POSTGRES_VERSION}).
     *
     * @return A configured {@link GenericContainer} instance ready to be started.
     */
    public static GenericContainer<?> newContainer() {
        return newContainer(DEFAULT_POSTGRES_VERSION);
    }

    /**
     * Creates a new PostgreSQL container using the specified version.
     * The container is pre-configured with:
     * <ul>
     *   <li>Port 5432 exposed</li>
     *   <li>Temporary filesystem for the data directory (avoids disk I/O in tests)</li>
     *   <li>{@code POSTGRES_HOST_AUTH_METHOD=trust} (no password required)</li>
     *   <li>Readiness wait on the "database system is ready to accept connections" log message</li>
     * </ul>
     *
     * @param version The PostgreSQL Docker image version tag (e.g. {@code "14"}, {@code "17"}).
     * @return A configured {@link GenericContainer} instance ready to be started.
     */
    public static GenericContainer<?> newContainer(String version) {
        return new GenericContainer<>("postgres:" + version)
                .withExposedPorts(5432)
                .withTmpFs(Collections.singletonMap("/var/lib/postgresql/data", "rw"))
                .withEnv("POSTGRES_HOST_AUTH_METHOD", "trust")
                /*
                 * A call to getConnection() when the postgres container is still not ready
                 * signals a PSQLException with message "FATAL: the database system is starting up".
                 * See: https://github.com/testcontainers/testcontainers-java/issues/317
                 */
                .waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*\\s", 2));
    }

    /**
     * Constructs a JDBC URL for a PostgreSQL database hosted in the given container.
     *
     * @param container A running PostgreSQL {@link GenericContainer} with port 5432 mapped.
     * @return JDBC URL of the form {@code jdbc:postgresql://<host>:<port>/postgres}.
     */
    public static String getJdbcUrl(GenericContainer<?> container) {
        return "jdbc:postgresql://" + container.getHost() + ":" + container.getMappedPort(5432) + "/postgres";
    }
}
