package org.evomaster.client.java.sql.dsl;

import org.evomaster.client.java.controller.api.dto.database.operations.InsertionDto;
import org.evomaster.client.java.controller.api.dto.database.operations.InsertionEntryDto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * DSL (Domain Specific Language) for operations on
 * the SQL Database
 */
public class SqlDsl implements SequenceDsl, StatementDsl {

    private List<InsertionDto> list = new ArrayList<>();

    private final List<InsertionDto> previousInsertionDtos = new ArrayList<>();

    private SqlDsl() {
    }

    private SqlDsl(List<InsertionDto>... previous) {
        if (previous != null && previous.length > 0){
            Arrays.stream(previous).forEach(previousInsertionDtos::addAll);
        }
    }

    /**
     * @return a DSL object to create SQL operations
     */
    public static SequenceDsl sql() {

        return new SqlDsl();
    }

    /**
     * @param previous a DSL object which is executed in the front of this
     * @return a DSL object to create SQL operations
     */
    public static SequenceDsl sql(List<InsertionDto>... previous) {

        return new SqlDsl(previous);
    }

    @Override
    public StatementDsl insertInto(String tableName, Long id) {

        checkDsl();

        if (tableName == null || tableName.isEmpty()) {
            throw new IllegalArgumentException("Unspecified table");
        }

        if (id != null && list.stream().anyMatch(t -> id.equals(t.id))) {
            throw new IllegalArgumentException("Non-unique id: " + id);
        }


        InsertionDto dto = new InsertionDto();
        dto.targetTable = tableName;
        dto.id = id;

        list.add(dto);

        return this;
    }

    @Override
    public StatementDsl d(String columnName, String printableValue) {

        checkDsl();

        if (columnName == null || columnName.isEmpty()) {
            throw new IllegalArgumentException("Unspecified variable");
        }

        InsertionEntryDto entry = new InsertionEntryDto();
        entry.variableName = columnName;
        entry.printableValue = printableValue;

        current().data.add(entry);

        return this;
    }

    @Override
    public StatementDsl r(String columnName, long insertionId) {
        checkDsl();

        if (columnName == null || columnName.isEmpty()) {
            throw new IllegalArgumentException("Unspecified variable");
        }

        if (list.stream().noneMatch(t -> t.id != null && t.id.equals(insertionId)) &&
                previousInsertionDtos.stream().noneMatch(t -> t.id != null && t.id.equals(insertionId))) {
            throw new IllegalArgumentException("Non-existing previous insertion with id: " + insertionId);
        }

        InsertionEntryDto entry = new InsertionEntryDto();
        entry.variableName = columnName;
        entry.foreignKeyToPreviouslyGeneratedRow = insertionId;

        current().data.add(entry);

        return this;

    }


    @Override
    public SequenceDsl and() {
        return this;
    }

    @Override
    public List<InsertionDto> dtos() {

        List<InsertionDto> tmp = list;
        list = null;

        return tmp;
    }


    private InsertionDto current() {
        return list.get(list.size() - 1);
    }

    private void checkDsl() {
        if (list == null) {
            throw new IllegalStateException("DTO was already built for this object");
        }
    }

}
