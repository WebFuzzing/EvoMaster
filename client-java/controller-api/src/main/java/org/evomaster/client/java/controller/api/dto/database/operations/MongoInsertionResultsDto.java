package org.evomaster.client.java.controller.api.dto.database.operations;

import java.util.ArrayList;
import java.util.List;

public class MongoInsertionResultsDto {
    /**
     * whether the insertion at the index of a sequence of Mongo insertions (i.e., {@link MongoDatabaseCommandDto#insertions})
     * executed successfully
     */
    public List<Boolean> executionResults = new ArrayList<>();
}
