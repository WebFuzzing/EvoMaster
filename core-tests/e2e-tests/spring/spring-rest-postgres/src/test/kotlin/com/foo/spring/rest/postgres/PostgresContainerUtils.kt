package com.foo.spring.rest.postgres

import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait

object PostgresContainerUtils {

    /**
     * Represents the version of the PostgreSQL database to be used for creating
     * new Postgres containers. This version string is utilized in defining the
     * container image (e.g., `postgres:14`) to ensure compatibility with application
     * requirements and configurations.
     *
     * This constant is particularly referenced in container creation where a specific
     * database version is necessary for testing or development purposes. It enables
     * consistency across environments and simplifies version management.
     */
    private const val POSTGRES_VERSION = "14"

    /**
     * Creates and configures a new instance of a Postgres container.
     * The container is pre-configured with a specific Postgres version, exposed ports,
     * authentication method, and a readiness check log message.
     *
     * @return A configured instance of GenericContainer for Postgres.
     */
    fun newContainer(): GenericContainer<*> {
        return GenericContainer<Nothing>("postgres:$POSTGRES_VERSION")
            .apply { withExposedPorts(5432) }
            .apply { withEnv("POSTGRES_HOST_AUTH_METHOD", "trust") }
            /*
             * A call to getConnection() when the postgres container is still not ready
             * signals a PSQLException with message "FATAL: the database system is starting up".
             * See: https://github.com/testcontainers/testcontainers-java/issues/317
             */
            .apply { waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*\\s", 2)) }
    }

    /**
     * Constructs the JDBC URL for connecting to a PostgreSQL database running inside a container.
     *
     * @param container the generic container instance representing the PostgreSQL database.
     * @return the JDBC URL as a string, formatted to connect to the database inside the given container.
     */
    fun getJdbcUrl(container: GenericContainer<*>): String {
        return "jdbc:postgresql://${container.host}:${container.getMappedPort(5432)}/postgres"
    }
}
