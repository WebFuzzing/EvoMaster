package org.evomaster.client.java.controller.api.dto.database.execution;

import java.util.Map;

public class FailedQuery {
    // Add database
    public FailedQuery(String collection, Class<?> documentsType, Map <String, Object> accessedFields) {
        this.collection = collection;
        this.documentsType = documentsType;
        this.accessedFields = accessedFields;
    }

    public FailedQuery(){
        this.collection = "";
    }

    private final String collection;
    private Class<?> documentsType;
    private Map<String, Object> accessedFields;

    public String getCollection() {
        return collection;
    }

    public  Map<String,Object> getAccessedFields() {
        return accessedFields;
    }

    public Class<?> getDocumentsType() {
        return documentsType;
    }
}
