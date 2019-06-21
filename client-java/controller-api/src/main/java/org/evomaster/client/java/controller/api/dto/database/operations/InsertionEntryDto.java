package org.evomaster.client.java.controller.api.dto.database.operations;

public class InsertionEntryDto {

    public String variableName;

    public String printableValue;

    /**
     * If non null, then printableValue should be null.
     * This should be an id of an InsertionDto previously
     * executed
     */
    public Long foreignKeyToPreviouslyGeneratedRow;

}
