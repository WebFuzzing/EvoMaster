package org.evomaster.client.java.controller.api.dto.database.operations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MongoInsertionResultsDto {
    /**
     * whether the insertion at the index of a sequence of Mongo insertions (i.e., {@link MongoDatabaseCommandDto#insertions})
     * executed successfully
     */
    public List<Boolean> executionResults = new ArrayList<>();
    public Integer failedInsertionIndex = -1;

    public Boolean insertionFailed() {
        return failedInsertionIndex != -1;
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
        Matcher matcher = Pattern.compile("index (\\d+)").matcher(e.getMessage());
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : -1;
    }
}
