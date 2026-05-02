package org.evomaster.client.java.controller.dynamodb.parsers;

import org.evomaster.client.java.controller.dynamodb.operations.QueryOperation;
import org.evomaster.client.java.instrumentation.DynamoDbOperationNames;

import java.util.Map;

/**
 * Parser for one DynamoDB API method family.
 */
public interface DynamoDbApiMethodParser {

    /**
     * Returns the DynamoDB API method handled by this parser.
     *
     * @return API method identifier
     */
    DynamoDbOperationNames apiMethodName();

    /**
     * Parses one request object into table-specific query operations.
     *
     * @param ddbRequest DynamoDB request object
     * @return a map of parsed operations by table name
     */
    Map<String, QueryOperation> parseRequest(Object ddbRequest);
}
