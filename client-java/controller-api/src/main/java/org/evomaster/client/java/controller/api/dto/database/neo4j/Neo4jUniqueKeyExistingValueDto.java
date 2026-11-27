package org.evomaster.client.java.controller.api.dto.database.neo4j;

import org.evomaster.client.java.controller.api.dto.database.operations.Neo4jInsertionEntryDto;

import java.util.ArrayList;
import java.util.List;

/**
 * A value of a unique key that a node already in the database holds, so inserted nodes must not
 * repeat it.
 */
public class Neo4jUniqueKeyExistingValueDto {

    /**
     * One entry per property of the key, in the same order as {@link Neo4jUniqueKeyDto#properties}.
     * A key on a single property has a single entry.
     */
    public List<Neo4jInsertionEntryDto> properties = new ArrayList<>();

    /**
     * Default constructor
     */
    public Neo4jUniqueKeyExistingValueDto() {
    }

    /**
     * @param properties one entry per property of the key, in the same order as the key's properties
     */
    public Neo4jUniqueKeyExistingValueDto(List<Neo4jInsertionEntryDto> properties) {
        this.properties = new ArrayList<>(properties);
    }
}
