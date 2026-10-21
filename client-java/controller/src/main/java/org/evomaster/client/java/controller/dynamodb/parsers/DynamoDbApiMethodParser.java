package org.evomaster.client.java.controller.dynamodb.parsers;

import org.evomaster.client.java.controller.dynamodb.ParsedDynamoDbRequest;
import org.evomaster.client.java.instrumentation.DynamoDbOperationNames;
import org.evomaster.client.java.instrumentation.DynamoDbQuery;

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
     * Parses one request object into table-specific request expressions.
     *
     * @param ddbQuery DynamoDB query data
     * @return a map of parsed requests by table name
     */
    Map<String, ParsedDynamoDbRequest> parseRequest(DynamoDbQuery ddbQuery);
}
