package org.evomaster.client.java.controller.api.dto.database.operations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InsertionResultsDto {

    /**
     * primary key generated with the insertions
     */
    public Map<Long, Long> idMapping = new HashMap<>();

    /**
     * whether the insertion at the index executed successfully
     */
    public List<Boolean> executionResults = new ArrayList<>();
}
