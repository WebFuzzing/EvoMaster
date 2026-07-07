package org.evomaster.client.java.sql;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.util.Collections;

public class PostgresContainerUtils {

    /**
     * Specifies the version of PostgreSQL used in the PostgreSQL container configuration.
     * This constant is utilized to define the version tag for the PostgreSQL Docker image.
     * It ensures consistency when creating new PostgreSQL container instances.
     */
    private static final String POSTGRES_VERSION = "14";

    /**
     * Creates and configures a new instance of a PostgreSQL container.
     * The container is pre-configured with the following settings:
     * - Exposes port 5432 for PostgreSQL.
     * - Sets up a temporary filesystem for the PostgreSQL data directory.
     * - Configures the environment variable "POSTGRES_HOST_AUTH_METHOD" with the value "trust".
     * - Waits until the database system is ready to accept connections by monitoring logs.
     *
     * @return A configured {@link GenericContainer} instance for a PostgreSQL database.
     */
    public static GenericContainer<?> newContainer() {
        return new GenericContainer<>("postgres:" + POSTGRES_VERSION)
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
     * Constructs a JDBC URL for a PostgreSQL database hosted within the specified container.
     *
     * @param container The {@link GenericContainer} instance representing a running PostgreSQL container.
     *                  It must be configured to expose and map the PostgreSQL default port (5432).
     *
     * @return The constructed JDBC URL in the format "jdbc:postgresql://<host>:<port>/postgres",
     *         where <host> and <port> are derived from the container instance.
     */
    public static String getJdbcUrl(GenericContainer<?> container) {
        return "jdbc:postgresql://" + container.getHost() + ":" + container.getMappedPort(5432) + "/postgres";
    }
}
