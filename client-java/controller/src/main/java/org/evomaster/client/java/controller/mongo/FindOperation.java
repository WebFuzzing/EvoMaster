package org.evomaster.client.java.controller.mongo;

import com.google.gson.Gson;
import org.bson.Document;
import org.evomaster.client.java.controller.api.dto.database.execution.FindOperationDto;

import java.util.Objects;

/**
 * A find(...) operation for a given database and collection over a MongoDB instance.
 */
public class FindOperation {

    public FindOperation(String databaseName, String collectionName, Document query) {
        Objects.requireNonNull(databaseName);
        Objects.requireNonNull(collectionName);
        Objects.requireNonNull(query);

        this.databaseName = databaseName;
        this.collectionName = collectionName;
        this.query = query;
    }

    private final String databaseName;

    private final String collectionName;

    private final Document query;

    public static FindOperation fromDto(FindOperationDto dto) {
        return new FindOperation(dto.databaseName,
                dto.collectionName,
                new Gson().fromJson(dto.queryJsonStr, Document.class));
    }

    public Document getQuery() {
        return query;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public FindOperationDto toDto() {
        FindOperationDto dto = new FindOperationDto();
        dto.databaseName = this.databaseName;
        dto.collectionName = this.collectionName;
        dto.queryJsonStr = new Gson().toJson(this.query);
        return dto;
    }
}
