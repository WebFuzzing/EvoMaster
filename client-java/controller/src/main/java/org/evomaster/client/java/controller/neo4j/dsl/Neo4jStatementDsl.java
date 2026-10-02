package org.evomaster.client.java.controller.neo4j.dsl;

import org.evomaster.client.java.controller.api.dto.database.operations.Neo4jDatabaseCommandsDto;

/**
 * Fluent definition of one Neo4j node or relationship.
 */
public interface Neo4jStatementDsl extends Neo4jSequenceDsl {

    /**
     * Adds a property using its printable representation. Strings must be enclosed in single quotes,
     * with any single quote inside them doubled. Numbers are represented by their exact text, and are
     * stored as integers. Booleans are represented by {@code true} or {@code false}.
     *
     * @param propertyKey name of the property
     * @param printableValue value in printable form
     * @return the continuation of this statement
     */
    Neo4jStatementDsl d(String propertyKey, String printableValue);

    /** @return the completed insertion commands */
    Neo4jDatabaseCommandsDto dtos();
}
