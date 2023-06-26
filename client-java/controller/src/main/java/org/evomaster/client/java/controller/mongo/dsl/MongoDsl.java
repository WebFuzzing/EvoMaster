package org.evomaster.client.java.controller.mongo.dsl;

import org.evomaster.client.java.controller.api.dto.database.operations.MongoInsertionDto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * DSL (Domain Specific Language) for operations on
 * the Mongo Database
 */
public class MongoDsl implements MongoSequenceDsl, MongoStatementDsl{

    private List<MongoInsertionDto> list = new ArrayList<>();

    private final List<MongoInsertionDto> previousInsertionDtos = new ArrayList<>();

    private MongoDsl() {
    }

    private MongoDsl(List<MongoInsertionDto>... previous) {
        if (previous != null && previous.length > 0){
            Arrays.stream(previous).forEach(previousInsertionDtos::addAll);
        }
    }

    /**
     * @return a DSL object to create MONGO operations
     */
    public static MongoSequenceDsl mongo() {
        return new MongoDsl();
    }

    /**
     * @param previous a DSL object which is executed in the front of this
     * @return a DSL object to create MONGO operations
     */
    public static MongoSequenceDsl mongo(List<MongoInsertionDto>... previous) {
        return new MongoDsl(previous);
    }

    @Override
    public MongoStatementDsl insertInto(String databaseName, String collectionName) {

        checkDsl();

        if (databaseName == null || databaseName.isEmpty()) {
            throw new IllegalArgumentException("Unspecified database");
        }

        if (collectionName == null || collectionName.isEmpty()) {
            throw new IllegalArgumentException("Unspecified collection");
        }

        MongoInsertionDto dto = new MongoInsertionDto();
        dto.databaseName = databaseName;
        dto.collectionName = collectionName;
        list.add(dto);

        return this;
    }

    @Override
    public MongoStatementDsl d(String printableValue) {
        checkDsl();
        current().data = printableValue;
        return this;
    }

    @Override
    public MongoSequenceDsl and() {
        return this;
    }

    @Override
    public List<MongoInsertionDto> dtos() {

        List<MongoInsertionDto> tmp = list;
        list = null;

        return tmp;
    }


    private MongoInsertionDto current() {
        return list.get(list.size() - 1);
    }

    private void checkDsl() {
        if (list == null) {
            throw new IllegalStateException("DTO was already built for this object");
        }
    }

}
