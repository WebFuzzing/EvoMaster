package org.evomaster.client.java.controller.api.dto.database.neo4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Data already in the SUT's Neo4j database that inserted nodes must not clash with.
 */
public class Neo4jExistingDataDto {

    /**
     * The keys the nodes inserted by EvoMaster must not repeat, together with the values already taken.
     */
    public List<Neo4jUniqueKeyDto> uniqueKeys = new ArrayList<>();

    /**
     * Default constructor
     */
    public Neo4jExistingDataDto() {
    }
}
