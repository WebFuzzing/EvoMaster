package org.evomaster.client.java.controller.api.dto.database.neo4j;

import org.evomaster.client.java.controller.api.dto.database.operations.Neo4jInsertionEntryDto;

import java.util.ArrayList;
import java.util.List;

/**
 * Properties whose values no two nodes with a label can share. It comes either from a uniqueness or
 * node key constraint of the database, or from the property the SUT's entity maps as its id, which the
 * SUT expects to be unique even when the database does not enforce it.
 */
public class Neo4jUniqueKeyDto {

    /**
     * Label of the nodes the key applies to.
     */
    public String label;

    /**
     * Names of the properties that make up the key. Usually just one, eg {@code id}. A composite
     * constraint, eg {@code REQUIRE (n.white, n.black) IS UNIQUE}, has several, and then two nodes clash
     * only when they repeat all of them.
     */
    public List<String> properties = new ArrayList<>();

    /**
     * The values already taken in the database, one element per existing node. Each element is a list
     * because a composite key has one value per property, in the same order as {@link #properties}.
     */
    public List<List<Neo4jInsertionEntryDto>> existingValues = new ArrayList<>();

    /**
     * Default constructor
     */
    public Neo4jUniqueKeyDto() {
    }

    /**
     * @param label label of the nodes the key applies to
     * @param properties names of the properties that make up the key
     */
    public Neo4jUniqueKeyDto(String label, List<String> properties) {
        this.label = label;
        this.properties = new ArrayList<>(properties);
    }
}
