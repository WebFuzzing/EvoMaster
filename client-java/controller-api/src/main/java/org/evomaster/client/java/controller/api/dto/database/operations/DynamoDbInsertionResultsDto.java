package org.evomaster.client.java.controller.api.dto.database.operations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Results of a sequence of DynamoDB insertions.
 */
public class DynamoDbInsertionResultsDto {

    public List<Boolean> executionResults = new ArrayList<>();
    public Integer failedInsertionIndex;

    /**
     * Records the insertion that failed while preserving earlier successes.
     *
     * @param insertions attempted insertions
     * @param failedIndex zero-based index of the failed insertion
     */
    public void handleFailedInsertion(List<DynamoDbInsertionDto> insertions, int failedIndex) {
        executionResults = new ArrayList<>(Collections.nCopies(insertions.size(), false));
        for (int i = 0; i < failedIndex; i++) {
            executionResults.set(i, true);
        }
        failedInsertionIndex = failedIndex;
    }
}
