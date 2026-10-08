package org.evomaster.client.java.controller.api.dto.database.operations;

import java.util.ArrayList;
import java.util.List;

/**
 * Results of a sequence of DynamoDB insertions.
 */
public class DynamoDbInsertionResultsDto {

    public List<Boolean> executionResults = new ArrayList<>();
    public Integer failedInsertionIndex;
}
