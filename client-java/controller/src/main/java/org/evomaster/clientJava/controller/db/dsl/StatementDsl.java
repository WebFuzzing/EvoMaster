package org.evomaster.clientJava.controller.db.dsl;

import org.evomaster.clientJava.controllerApi.dto.database.InsertionDto;

import java.util.List;

public interface StatementDsl {


    StatementDsl d(String variableName, String printableValue);

    StatementDsl r(String variableName, int insertionId);

    SequenceDsl and();

    /**
     * Build the DTOs (Data Transfer Object) from this DSL,
     * closing it (ie, not usable any longer).
     * @return
     */
    List<InsertionDto> dtos();

}
