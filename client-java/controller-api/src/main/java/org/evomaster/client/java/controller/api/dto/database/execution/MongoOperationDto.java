package org.evomaster.client.java.controller.api.dto.database.execution;

public class MongoOperationDto {

    public enum Type {MONGO_FIND}

    public Type operationType;

    public String operationJsonStr;

    public boolean hasOperationFoundAnyDocuments;
}
