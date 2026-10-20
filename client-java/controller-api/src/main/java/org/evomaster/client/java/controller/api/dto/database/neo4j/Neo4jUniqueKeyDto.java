package org.evomaster.client.java.controller.api.dto.database.neo4j;

import org.evomaster.client.java.controller.api.dto.database.operations.Neo4jInsertionEntryDto;

import java.util.ArrayList;
import java.util.List;

/**
 * A set of properties whose values no two nodes with a label can share. It comes either from a
 * uniqueness or node key constraint of the database, or from the property the SUT's entity maps as
 * its id, which the SUT expects to be unique even when the database does not enforce it.
 */
public class Neo4jUniqueKeyDto {

    /**
     * Label of the nodes the key applies to.
     */
    public String label;

    /**
     * Properties that together identify a node with {@link #label}. A node that lacks any of them is
     * not bound by the key.
     */
    public List<String> propertyKeys = new ArrayList<>();

    /**
     * The values the nodes already in the database hold for the key, one list per node, with one entry
     * per property of {@link #propertyKeys} in the same order.
     */
    public List<List<Neo4jInsertionEntryDto>> existingValues = new ArrayList<>();

    /**
     * Default constructor
     */
    public Neo4jUniqueKeyDto() {
    }
}
