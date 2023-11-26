package org.evomaster.client.java.controller.api.dto.database.operations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MongoInsertionResultsDto {
    /**
     * Whether the insertion at the index of a sequence of Mongo insertions (i.e., {@link MongoDatabaseCommandDto#insertions})
     * executed successfully
     */
    public List<Boolean> executionResults = new ArrayList<>();
    /**
     * The index of the insertion that failed if any
     */
    public Integer failedInsertionIndex = -1;
    /**
     * Regex to extract index of failed insertion from the exception message thrown by the executeInsertion method in MongoScriptRunner
     */
    private static final Pattern pattern = Pattern.compile("index (\\d+)");

    public Boolean insertionFailed() {
        return failedInsertionIndex >= 0;
    }

    public void handleFailedInsertion(List<MongoInsertionDto> insertions, Exception e) {
        failedInsertionIndex = findFailedInsertion(e);

        if(insertionFailed()){
            List<Boolean> results = new ArrayList<>(Collections.nCopies(insertions.size(), false));

            for (int i = 0; i < failedInsertionIndex; i++) {
                results.set(i, true);
            }

            executionResults = results;
        }
    }

    private static int findFailedInsertion(Exception e) {
        Matcher matcher = pattern.matcher(e.getMessage());
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : -1;
    }
}
