package org.evomaster.client.java.controller.dynamodb.parsers;

import org.evomaster.client.java.controller.dynamodb.operations.QueryOperation;
import org.evomaster.client.java.instrumentation.DynamoDbOperationNames;

import java.util.Map;

/**
 * Parser for one DynamoDB API method family.
 */
public interface DynamoDbApiMethodParser {

    DynamoDbOperationNames apiMethodName();

    Map<String, QueryOperation> parseRequest(Object request);
}
