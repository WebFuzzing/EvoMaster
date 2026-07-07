package org.evomaster.core

import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy
import java.util.*

object PostgresContainerUtils {

    /**
     * Default version of PostgreSQL used for testing and database-related operations.
     * This constant is utilized in scenarios where no specific PostgreSQL version is explicitly provided,
     * ensuring a consistent version is used across various tests and utilities.
     */
    private const val DEFAULT_POSTGRES_VERSION = "14"

    /**
     * Creates and configures a new PostgreSQL container.
     *
     * @param version Specifies the PostgreSQL version to be used for the container. Defaults to `DEFAULT_POSTGRES_VERSION`.
     * @return A configured instance of `KGenericContainer` running the specified PostgreSQL version.
     */
    fun newContainer(version: String = DEFAULT_POSTGRES_VERSION): KGenericContainer {
        return KGenericContainer("postgres:$version")
            .withExposedPorts(5432)
            //https://www.postgresql.org/docs/current/auth-trust.html
            .withEnv("POSTGRES_HOST_AUTH_METHOD", "trust")
            .withTmpFs(Collections.singletonMap("/var/lib/postgresql/data", "rw"))
            /*
             * A call to getConnection() when the postgres container is still not ready
             * signals a PSQLException with message "FATAL: the database system is starting up".
             * See: https://github.com/testcontainers/testcontainers-java/issues/317
             */
            .waitingFor(
                LogMessageWaitStrategy()
                    .withRegEx(".*database system is ready to accept connections.*\\s")
                    .withTimes(2)
            )
    }

    /**
     * Constructs a JDBC URL for a PostgreSQL database based on the provided container's host and mapped port.
     *
     * @param container the container from which the host and mapped port will be obtained
     * @return a JDBC URL string for connecting to the PostgreSQL database
     */
    fun getJdbcUrl(container: KGenericContainer): String {
        val host = container.host
        val port = container.getMappedPort(5432)!!
        return "jdbc:postgresql://$host:$port/postgres"
    }
}
