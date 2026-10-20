package org.evomaster.client.java.controller.api.dto.database.neo4j;

import java.util.ArrayList;
import java.util.List;

/**
 * What the controller read from the SUT's Neo4j database when the search started.
 */
public class Neo4jDbInfoDto {

    /**
     * The keys the nodes inserted by EvoMaster must not repeat, together with the values already taken.
     */
    public List<Neo4jUniqueKeyDto> uniqueKeys = new ArrayList<>();

    /**
     * Default constructor
     */
    public Neo4jDbInfoDto() {
    }
}
