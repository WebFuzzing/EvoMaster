package org.evomaster.clientJava.controllerApi.dto.database;

public class InsertionEntryDto {

    public String variableName;

    public String printableValue;

    /**
     * If non null, then printableValue should be null
     */
    public Integer foreignKeyToPreviouslyGeneratedRow;
}
