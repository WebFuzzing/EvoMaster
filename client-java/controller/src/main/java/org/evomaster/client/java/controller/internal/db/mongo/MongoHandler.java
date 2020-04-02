package org.evomaster.client.java.controller.internal.db.mongo;

import com.google.gson.Gson;
import org.evomaster.client.java.controller.api.dto.mongo.ExecutedFindOperationDto;
import org.evomaster.client.java.controller.api.dto.mongo.FindOperationDto;
import org.evomaster.client.java.controller.api.dto.mongo.FindResultDto;
import org.evomaster.client.java.controller.api.dto.mongo.MongoExecutionDto;
import org.evomaster.client.java.instrumentation.mongo.LoggedExecutedFindOperation;

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
        dto.executedFindOperationDtos.addAll(mongoOperations.stream().map(MongoHandler::toDto).collect(Collectors.toList()));
        return dto;
    }

    private static ExecutedFindOperationDto toDto(String loggedExecutedFindOperationJsonStr) {
        LoggedExecutedFindOperation loggedExecutedFindOperation = new Gson().fromJson(loggedExecutedFindOperationJsonStr, LoggedExecutedFindOperation.class);

        FindOperationDto findOperationDto = new FindOperationDto();
        findOperationDto.databaseName = loggedExecutedFindOperation.getDatabaseName();
        findOperationDto.collectionName = loggedExecutedFindOperation.getCollectionName();
        findOperationDto.queryJsonStr =  new Gson().toJson(loggedExecutedFindOperation.getQueryDocument());

        FindResultDto findResultDto = new FindResultDto();
        findResultDto.findResultType = FindResultDto.FindResultType.SUMMARY;
        findResultDto.hasReturnedAnyDocument = loggedExecutedFindOperation.hasReturnedAnyDocument();

        ExecutedFindOperationDto dto = new ExecutedFindOperationDto();
        dto.findOperationDto = findOperationDto;
        dto.findResultDto = findResultDto;
        return dto;
    }
}