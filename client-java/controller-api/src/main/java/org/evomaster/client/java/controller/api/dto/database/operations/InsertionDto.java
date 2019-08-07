package org.evomaster.client.java.controller.api.dto.database.operations;

import java.util.ArrayList;
import java.util.List;

public class InsertionDto {

    /**
     * The ID of this insertion operation.
     * This is needed when we have multiple insertions, where
     * we need to refer (eg foreign key) to the data generated
     * by a previous insertion.
     * It can be null.
     */
    public Long id;

    public String targetTable;

    public List<InsertionEntryDto> data = new ArrayList<>();
}
