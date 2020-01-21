package org.evomaster.client.java.controller.internal.db.mongo;

import org.evomaster.client.java.controller.api.dto.database.execution.MongoExecutionDto;
import org.evomaster.client.java.controller.api.dto.database.execution.MongoOperationDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * The client-side MongoHandler basically records interactions
 * between the client and the MongoDB database
 */
public class MongoHandler {

    private final List<String> mongoOperations;

    private volatile boolean extractMongoExecution;

    public MongoHandler() {
        mongoOperations = new ArrayList<>();
    }

    public void reset() {
        mongoOperations.clear();
    }

    public void handle(String mongoOperation) {
        Objects.requireNonNull(mongoOperation);


        if (!extractMongoExecution) {
            return;
        }

        this.mongoOperations.add(mongoOperation);
    }


    public boolean isExtractMongoExecution() {
        return extractMongoExecution;
    }


    public void setExtractMongoExecution(boolean extractMongoExecution) {
        this.extractMongoExecution = extractMongoExecution;
    }

    public MongoExecutionDto getMongoExecutionDto() {
        if (!extractMongoExecution) {
            return null;
        }

        MongoExecutionDto dto = new MongoExecutionDto();
        dto.mongoOperations.addAll(mongoOperations.stream().map(MongoHandler::toDto).collect(Collectors.toList()));
        return dto;
    }

    private static MongoOperationDto toDto(String op) {
        MongoOperationDto dto = new MongoOperationDto();
        dto.operationType = MongoOperationDto.Type.MONGO_FIND;
        dto.operationJsonStr = op;
        return dto;
    }
}