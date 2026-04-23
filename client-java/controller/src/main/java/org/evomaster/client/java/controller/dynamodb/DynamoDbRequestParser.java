package org.evomaster.client.java.controller.dynamodb;

import org.evomaster.client.java.controller.dynamodb.operations.QueryOperation;
import org.evomaster.client.java.controller.dynamodb.parsers.*;
import org.evomaster.client.java.instrumentation.DynamoDbOperationNames;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builds internal operations from DynamoDB request objects.
 */
public class DynamoDbRequestParser {

    private final Map<DynamoDbOperationNames, DynamoDbApiMethodParser> parsersByApiMethod;

    public DynamoDbRequestParser() {
        Map<DynamoDbOperationNames, DynamoDbApiMethodParser> map = new LinkedHashMap<>();
        registerParser(map, new QueryApiMethodParser());
        registerParser(map, new ScanApiMethodParser());
        registerParser(map, new GetItemApiMethodParser());
        registerParser(map, new BatchGetItemApiMethodParser());
        registerParser(map, new PutItemApiMethodParser());
        registerParser(map, new DeleteItemApiMethodParser());
        registerParser(map, new UpdateItemApiMethodParser());
        this.parsersByApiMethod = Collections.unmodifiableMap(map);
    }

    /**
     * Entry-point parser used by the handler.
     * It routes a DynamoDB SDK request to the API-method parser and returns
     * one parsed condition tree per table name.
     * Unsupported operations intentionally yield an empty map.
     */
    public Map<String, QueryOperation> parseByTable(Object request, DynamoDbOperationNames apiMethodName) {
        if (request == null || apiMethodName == null) {
            return Collections.emptyMap();
        }

        DynamoDbApiMethodParser parser = parsersByApiMethod.get(apiMethodName);
        if (parser == null) {
            return Collections.emptyMap();
        }

        Map<String, QueryOperation> parsed = parser.parseRequest(request);
        return parsed == null ? Collections.emptyMap() : parsed;
    }

    private static void registerParser(Map<DynamoDbOperationNames, DynamoDbApiMethodParser> parsersByApiMethod,
                                       DynamoDbApiMethodParser parser) {
        DynamoDbOperationNames apiMethodName = parser.apiMethodName();
        if (apiMethodName == null) {
            throw new IllegalArgumentException("Parser api method name cannot be null or blank");
        }

        DynamoDbApiMethodParser previous = parsersByApiMethod.put(apiMethodName, parser);
        if (previous != null) {
            throw new IllegalStateException("Duplicate parser for api method " + apiMethodName);
        }
    }
}
