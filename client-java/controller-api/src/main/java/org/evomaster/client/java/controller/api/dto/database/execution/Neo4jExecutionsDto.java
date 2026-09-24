package org.evomaster.client.java.controller.api.dto.database.execution;

import java.util.ArrayList;
import java.util.List;

/**
 * The MATCH queries run during one action that the graph did not satisfy, each digested into the
 * insertion that would satisfy it.
 */
public class Neo4jExecutionsDto {

    public List<Neo4jFailedQuery> failedQueries = new ArrayList<>();

    public Neo4jExecutionsDto() {
    }
}
