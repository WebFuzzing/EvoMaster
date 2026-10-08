package org.evomaster.client.java.controller.api.dto.database.execution;

import java.util.ArrayList;
import java.util.List;

/**
 * DynamoDB reads that can be satisfied by generated initialization data.
 */
public class DynamoDbExecutionsDto {

    public List<DynamoDbFailedQuery> failedQueries = new ArrayList<>();

    public DynamoDbExecutionsDto() {
    }
}
