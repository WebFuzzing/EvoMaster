package org.evomaster.client.java.controller.api.dto.database.operations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CassandraInsertionResultsDto {
    /**
     * Whether the insertion at the index of a sequence of Cassandra insertions (i.e., {@link CassandraDatabaseCommandDto#insertions})
     * executed successfully
     */
    public List<Boolean> executionResults = new ArrayList<>();

    /**
     * The index of the insertion that failed if any
     */
    public Integer failedInsertionIndex = -1;

    /**
     * Regex to extract index of failed insertion from the exception message thrown by the executeInsert method in CassandraScriptRunner
     */
    private static final Pattern pattern = Pattern.compile("index (\\d+)");

    public Boolean insertionFailed() {
        return failedInsertionIndex >= 0;
    }

    /**
     * Given the exception thrown when executing a sequence of Cassandra insertions, extract the index of the
     * insertion that failed, and mark as successfully executed only the insertions that came before it.
     * <p>
     * To be used in EMController once the insertion of Cassandra data is implemented, in the same way as it is
     * currently done for Mongo in executeMongoInsertion, ie to build the results DTO to send back when
     * CassandraScriptRunner#executeInsert throws an exception.
     *
     * @param insertions the sequence of insertions that was attempted
     * @param e          the exception thrown while executing such insertions
     */
    public void handleFailedInsertion(List<CassandraInsertionDto> insertions, Exception e) {
        failedInsertionIndex = findFailedInsertion(e);

        if (insertionFailed()) {
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
