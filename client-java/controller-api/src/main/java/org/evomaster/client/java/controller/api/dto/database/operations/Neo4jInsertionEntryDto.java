package org.evomaster.client.java.controller.api.dto.database.operations;

/**
 * A single property to set on an inserted Neo4j node or relationship.
 */
public class Neo4jInsertionEntryDto {

    /** Name of the property. */
    public String propertyKey;

    /** Type the property is stored with, which decides how {@link #value} is read. */
    public Neo4jPropertyTypeDto type;

    public String value;

    public Neo4jInsertionEntryDto() {
    }

    /**
     * Creates a property entry.
     *
     * @param propertyKey name of the property
     * @param type type the property is stored with
     * @param value the value as text
     */
    public Neo4jInsertionEntryDto(String propertyKey, Neo4jPropertyTypeDto type, String value) {
        this.propertyKey = propertyKey;
        this.type = type;
        this.value = value;
    }
}
