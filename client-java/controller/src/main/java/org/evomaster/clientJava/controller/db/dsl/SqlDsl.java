package org.evomaster.clientJava.controller.db.dsl;

import org.evomaster.clientJava.controllerApi.dto.database.operations.InsertionDto;
import org.evomaster.clientJava.controllerApi.dto.database.operations.InsertionEntryDto;

import java.util.ArrayList;
import java.util.List;

/**
 * DSL (Domain Specific Language) for operations on
 * the SQL Database
 */
public class SqlDsl implements SequenceDsl, StatementDsl{

    private List<InsertionDto> list = new ArrayList<>();

    private SqlDsl(){
    }

    /**
     *
     * @return a DSL object to create SQL operations
     */
    public static SequenceDsl sql(){

        return new SqlDsl();
    }

    @Override
    public StatementDsl insertInto(String tableName, Long id){

        checkDsl();

        if(tableName == null || tableName.isEmpty()){
            throw new IllegalArgumentException("Unspecified table");
        }

        if(id != null && list.stream().anyMatch(t -> id.equals(t.id))){
            throw new IllegalArgumentException("Non-unique id: " + id);
        }


        InsertionDto dto = new InsertionDto();
        dto.targetTable = tableName;
        dto.id = id;

        list.add(dto);

        return this;
    }

    @Override
    public StatementDsl d(String variableName, String printableValue){

        checkDsl();

        if(variableName == null || variableName.isEmpty()){
            throw new IllegalArgumentException("Unspecified variable");
        }

        InsertionEntryDto entry = new InsertionEntryDto();
        entry.variableName = variableName;
        entry.printableValue = printableValue;

        current().data.add(entry);

        return this;
    }

    @Override
    public StatementDsl r(String variableName, long insertionId){

        checkDsl();

        if(variableName == null || variableName.isEmpty()){
            throw new IllegalArgumentException("Unspecified variable");
        }

        if(list.stream().noneMatch(t -> t.id!=null && t.id.equals(insertionId))){
            throw new IllegalArgumentException("Non-existing previous insertion with id: " + insertionId);
        }

        InsertionEntryDto entry = new InsertionEntryDto();
        entry.variableName = variableName;
        entry.foreignKeyToPreviouslyGeneratedRow = (long) insertionId;

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


    private InsertionDto current(){
        return list.get(list.size() - 1);
    }

    private void checkDsl(){
        if(list == null){
            throw new IllegalStateException("DTO was already built for this object");
        }
    }

}
