package org.evomaster.client.java.controller.api.dto.database.execution;

import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbAttributeValueDto;

import java.util.ArrayList;
import java.util.List;

/**
 * Equality constraints from a successful DynamoDB read that returned no matching item.
 */
public class DynamoDbFailedQuery {

    public String tableName;
    public List<DynamoDbAttributeValueDto> attributes = new ArrayList<>();

    public DynamoDbFailedQuery() {
    }

    /**
     * Creates a failed-query description.
     *
     * @param tableName target table
     * @param attributes equality-constrained attributes
     */
    public DynamoDbFailedQuery(String tableName, List<DynamoDbAttributeValueDto> attributes) {
        this.tableName = tableName;
        this.attributes = new ArrayList<>(attributes);
    }
}
