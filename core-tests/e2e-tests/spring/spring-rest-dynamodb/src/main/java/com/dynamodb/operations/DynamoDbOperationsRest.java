package com.dynamodb.operations;

import com.dynamodb.operations.DynamoDbOperationsData.ClientMode;
import com.dynamodb.operations.DynamoDbOperationsData.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.KeysAndAttributes;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * REST endpoints covering every DynamoDB operation intercepted by EvoMaster.
 */
@RestController
@RequestMapping("/operations")
public class DynamoDbOperationsRest {

    private final DynamoDbClient syncClient;
    private final DynamoDbAsyncClient asyncClient;

    /**
     * Creates the operation matrix REST controller.
     *
     * @param syncClient synchronous DynamoDB client
     * @param asyncClient asynchronous DynamoDB client
     */
    public DynamoDbOperationsRest(DynamoDbClient syncClient, DynamoDbAsyncClient asyncClient) {
        this.syncClient = syncClient;
        this.asyncClient = asyncClient;
    }

    /**
     * Executes synchronous GetItem.
     *
     * @param existingPlayer whether the canonical player should be requested
     * @return operation outcome
     */
    @GetMapping("/sync/get-item/{existingPlayer}")
    public ResponseEntity<String> getItemSync(@PathVariable boolean existingPlayer) {
        return getItem(ClientMode.SYNC, existingPlayer);
    }

    /**
     * Executes asynchronous GetItem.
     *
     * @param existingPlayer whether the canonical player should be requested
     * @return operation outcome
     */
    @GetMapping("/async/get-item/{existingPlayer}")
    public ResponseEntity<String> getItemAsync(@PathVariable boolean existingPlayer) {
        return getItem(ClientMode.ASYNC, existingPlayer);
    }

    /**
     * Executes synchronous BatchGetItem.
     *
     * @param existingPlayer whether the canonical player should be requested
     * @return operation outcome
     */
    @GetMapping("/sync/batch-get-item/{existingPlayer}")
    public ResponseEntity<String> batchGetItemSync(@PathVariable boolean existingPlayer) {
        return batchGetItem(ClientMode.SYNC, existingPlayer);
    }

    /**
     * Executes asynchronous BatchGetItem.
     *
     * @param existingPlayer whether the canonical player should be requested
     * @return operation outcome
     */
    @GetMapping("/async/batch-get-item/{existingPlayer}")
    public ResponseEntity<String> batchGetItemAsync(@PathVariable boolean existingPlayer) {
        return batchGetItem(ClientMode.ASYNC, existingPlayer);
    }

    /**
     * Executes synchronous PutItem.
     *
     * @param existingPlayer whether the canonical player should be requested
     * @return operation outcome
     */
    @PostMapping("/sync/put-item/{existingPlayer}")
    public ResponseEntity<String> putItemSync(@PathVariable boolean existingPlayer) {
        return putItem(ClientMode.SYNC, existingPlayer);
    }

    /**
     * Executes asynchronous PutItem.
     *
     * @param existingPlayer whether the canonical player should be requested
     * @return operation outcome
     */
    @PostMapping("/async/put-item/{existingPlayer}")
    public ResponseEntity<String> putItemAsync(@PathVariable boolean existingPlayer) {
        return putItem(ClientMode.ASYNC, existingPlayer);
    }

    /**
     * Executes synchronous UpdateItem.
     *
     * @param existingPlayer whether the canonical player should be requested
     * @return operation outcome
     */
    @PutMapping("/sync/update-item/{existingPlayer}")
    public ResponseEntity<String> updateItemSync(@PathVariable boolean existingPlayer) {
        return updateItem(ClientMode.SYNC, existingPlayer);
    }

    /**
     * Executes asynchronous UpdateItem.
     *
     * @param existingPlayer whether the canonical player should be requested
     * @return operation outcome
     */
    @PutMapping("/async/update-item/{existingPlayer}")
    public ResponseEntity<String> updateItemAsync(@PathVariable boolean existingPlayer) {
        return updateItem(ClientMode.ASYNC, existingPlayer);
    }

    /**
     * Executes synchronous DeleteItem.
     *
     * @param existingPlayer whether the canonical player should be requested
     * @return operation outcome
     */
    @DeleteMapping("/sync/delete-item/{existingPlayer}")
    public ResponseEntity<String> deleteItemSync(@PathVariable boolean existingPlayer) {
        return deleteItem(ClientMode.SYNC, existingPlayer);
    }

    /**
     * Executes asynchronous DeleteItem.
     *
     * @param existingPlayer whether the canonical player should be requested
     * @return operation outcome
     */
    @DeleteMapping("/async/delete-item/{existingPlayer}")
    public ResponseEntity<String> deleteItemAsync(@PathVariable boolean existingPlayer) {
        return deleteItem(ClientMode.ASYNC, existingPlayer);
    }

    /**
     * Executes synchronous Query.
     *
     * @param existingPlayer whether the canonical player should be requested
     * @return operation outcome
     */
    @GetMapping("/sync/query/{existingPlayer}")
    public ResponseEntity<String> querySync(@PathVariable boolean existingPlayer) {
        return query(ClientMode.SYNC, existingPlayer);
    }

    /**
     * Executes asynchronous Query.
     *
     * @param existingPlayer whether the canonical player should be requested
     * @return operation outcome
     */
    @GetMapping("/async/query/{existingPlayer}")
    public ResponseEntity<String> queryAsync(@PathVariable boolean existingPlayer) {
        return query(ClientMode.ASYNC, existingPlayer);
    }

    /**
     * Executes synchronous Scan.
     *
     * @param existingPlayer whether the canonical player should be requested
     * @return operation outcome
     */
    @GetMapping("/sync/scan/{existingPlayer}")
    public ResponseEntity<String> scanSync(@PathVariable boolean existingPlayer) {
        return scan(ClientMode.SYNC, existingPlayer);
    }

    /**
     * Executes asynchronous Scan.
     *
     * @param existingPlayer whether the canonical player should be requested
     * @return operation outcome
     */
    @GetMapping("/async/scan/{existingPlayer}")
    public ResponseEntity<String> scanAsync(@PathVariable boolean existingPlayer) {
        return scan(ClientMode.ASYNC, existingPlayer);
    }

    /**
     * Reads one player with GetItem.
     *
     * @param clientMode SDK client variant
     * @param existingPlayer whether the canonical player should be requested
     * @return operation outcome
     */
    private ResponseEntity<String> getItem(ClientMode clientMode, boolean existingPlayer) {
        GetItemRequest request = GetItemRequest.builder()
                .tableName(DynamoDbOperationsData.TABLE_NAME)
                .key(DynamoDbOperationsData.key(clientMode, Operation.GET_ITEM, shirtNumber(existingPlayer)))
                .build();
        GetItemResponse response = execute(clientMode,
                () -> syncClient.getItem(request),
                () -> asyncClient.getItem(request));
        return readOutcome(clientMode, Operation.GET_ITEM, !response.item().isEmpty());
    }

    /**
     * Reads one player with BatchGetItem.
     *
     * @param clientMode SDK client variant
     * @param existingPlayer whether the canonical player should be requested
     * @return operation outcome
     */
    private ResponseEntity<String> batchGetItem(ClientMode clientMode, boolean existingPlayer) {
        KeysAndAttributes keys = KeysAndAttributes.builder()
                .keys(Collections.singletonList(
                        DynamoDbOperationsData.key(
                                clientMode, Operation.BATCH_GET_ITEM, shirtNumber(existingPlayer))))
                .build();
        BatchGetItemRequest request = BatchGetItemRequest.builder()
                .requestItems(Collections.singletonMap(DynamoDbOperationsData.TABLE_NAME, keys))
                .build();
        BatchGetItemResponse response = execute(clientMode,
                () -> syncClient.batchGetItem(request),
                () -> asyncClient.batchGetItem(request));
        boolean found = !response.responses()
                .getOrDefault(DynamoDbOperationsData.TABLE_NAME, Collections.emptyList()).isEmpty();
        return readOutcome(clientMode, Operation.BATCH_GET_ITEM, found);
    }

    /**
     * Replaces one player with PutItem when its shirt number matches.
     *
     * @param clientMode SDK client variant
     * @param existingPlayer whether the canonical player should be requested
     * @return operation outcome
     */
    private ResponseEntity<String> putItem(ClientMode clientMode, boolean existingPlayer) {
        Map<String, String> names = nameMap(DynamoDbOperationsData.SHIRT_NUMBER_ATTRIBUTE);
        Map<String, AttributeValue> values = valueMap(":shirtNumber", shirtNumber(existingPlayer));
        PutItemRequest request = PutItemRequest.builder()
                .tableName(DynamoDbOperationsData.TABLE_NAME)
                .item(DynamoDbOperationsData.item(clientMode, Operation.PUT_ITEM))
                .conditionExpression("#field = :shirtNumber")
                .expressionAttributeNames(names)
                .expressionAttributeValues(values)
                .build();
        return conditionalWrite(clientMode, Operation.PUT_ITEM, 201,
                () -> syncClient.putItem(request),
                () -> asyncClient.putItem(request));
    }

    /**
     * Updates one player with UpdateItem when its shirt number matches.
     *
     * @param clientMode SDK client variant
     * @param existingPlayer whether the canonical player should be requested
     * @return operation outcome
     */
    private ResponseEntity<String> updateItem(ClientMode clientMode, boolean existingPlayer) {
        Map<String, String> names = new HashMap<>();
        names.put("#shirtNumber", DynamoDbOperationsData.SHIRT_NUMBER_ATTRIBUTE);
        names.put("#playerName", DynamoDbOperationsData.PLAYER_NAME_ATTRIBUTE);
        Map<String, AttributeValue> values = valueMap(":shirtNumber", shirtNumber(existingPlayer));
        values.put(":playerName", AttributeValue.builder().s("Lionel Messi").build());
        UpdateItemRequest request = UpdateItemRequest.builder()
                .tableName(DynamoDbOperationsData.TABLE_NAME)
                .key(DynamoDbOperationsData.key(
                        clientMode, Operation.UPDATE_ITEM, DynamoDbOperationsData.TARGET_SHIRT_NUMBER))
                .updateExpression("SET #playerName = :playerName")
                .conditionExpression("#shirtNumber = :shirtNumber")
                .expressionAttributeNames(names)
                .expressionAttributeValues(values)
                .build();
        return conditionalWrite(clientMode, Operation.UPDATE_ITEM, 200,
                () -> syncClient.updateItem(request),
                () -> asyncClient.updateItem(request));
    }

    /**
     * Deletes one player with DeleteItem when its shirt number matches.
     *
     * @param clientMode SDK client variant
     * @param existingPlayer whether the canonical player should be requested
     * @return operation outcome
     */
    private ResponseEntity<String> deleteItem(ClientMode clientMode, boolean existingPlayer) {
        DeleteItemRequest request = DeleteItemRequest.builder()
                .tableName(DynamoDbOperationsData.TABLE_NAME)
                .key(DynamoDbOperationsData.key(
                        clientMode, Operation.DELETE_ITEM, DynamoDbOperationsData.TARGET_SHIRT_NUMBER))
                .conditionExpression("#field = :shirtNumber")
                .expressionAttributeNames(nameMap(DynamoDbOperationsData.SHIRT_NUMBER_ATTRIBUTE))
                .expressionAttributeValues(valueMap(":shirtNumber", shirtNumber(existingPlayer)))
                .build();
        try {
            execute(clientMode,
                    () -> syncClient.deleteItem(request),
                    () -> asyncClient.deleteItem(request));
            restoreDeletedItem(clientMode);
            return outcome(clientMode, Operation.DELETE_ITEM, true, 200, 409);
        } catch (RuntimeException exception) {
            if (hasConditionalCheckFailure(exception)) {
                return outcome(clientMode, Operation.DELETE_ITEM, false, 200, 409);
            }
            throw exception;
        }
    }

    /**
     * Reads one player with Query.
     *
     * @param clientMode SDK client variant
     * @param existingPlayer whether the canonical player should be requested
     * @return operation outcome
     */
    private ResponseEntity<String> query(ClientMode clientMode, boolean existingPlayer) {
        Map<String, String> names = new HashMap<>();
        names.put("#scenario", DynamoDbOperationsData.SCENARIO_ATTRIBUTE);
        names.put("#shirtNumber", DynamoDbOperationsData.SHIRT_NUMBER_ATTRIBUTE);
        Map<String, AttributeValue> values = new HashMap<>();
        values.put(":scenario", AttributeValue.builder()
                .s(DynamoDbOperationsData.scenario(clientMode, Operation.QUERY)).build());
        values.put(":shirtNumber", AttributeValue.builder()
                .n(Integer.toString(shirtNumber(existingPlayer))).build());
        QueryRequest request = QueryRequest.builder()
                .tableName(DynamoDbOperationsData.TABLE_NAME)
                .keyConditionExpression("#scenario = :scenario AND #shirtNumber = :shirtNumber")
                .expressionAttributeNames(names)
                .expressionAttributeValues(values)
                .build();
        QueryResponse response = execute(clientMode,
                () -> syncClient.query(request),
                () -> asyncClient.query(request));
        return readOutcome(clientMode, Operation.QUERY, !response.items().isEmpty());
    }

    /**
     * Reads one player with Scan.
     *
     * @param clientMode SDK client variant
     * @param existingPlayer whether the canonical player should be requested
     * @return operation outcome
     */
    private ResponseEntity<String> scan(ClientMode clientMode, boolean existingPlayer) {
        Map<String, String> names = new HashMap<>();
        names.put("#scenario", DynamoDbOperationsData.SCENARIO_ATTRIBUTE);
        names.put("#shirtNumber", DynamoDbOperationsData.SHIRT_NUMBER_ATTRIBUTE);
        Map<String, AttributeValue> values = new HashMap<>();
        values.put(":scenario", AttributeValue.builder()
                .s(DynamoDbOperationsData.scenario(clientMode, Operation.SCAN)).build());
        values.put(":shirtNumber", AttributeValue.builder()
                .n(Integer.toString(shirtNumber(existingPlayer))).build());
        ScanRequest request = ScanRequest.builder()
                .tableName(DynamoDbOperationsData.TABLE_NAME)
                .filterExpression("#scenario = :scenario AND #shirtNumber = :shirtNumber")
                .expressionAttributeNames(names)
                .expressionAttributeValues(values)
                .build();
        ScanResponse response = execute(clientMode,
                () -> syncClient.scan(request),
                () -> asyncClient.scan(request));
        return readOutcome(clientMode, Operation.SCAN, !response.items().isEmpty());
    }

    /**
     * Executes an SDK call through the selected client variant.
     *
     * @param clientMode selected SDK client
     * @param syncCall synchronous call
     * @param asyncCall asynchronous call
     * @param <T> response type
     * @return SDK response
     */
    private <T> T execute(
            ClientMode clientMode, Supplier<T> syncCall, Supplier<CompletableFuture<T>> asyncCall) {
        return clientMode == ClientMode.SYNC ? syncCall.get() : asyncCall.get().join();
    }

    /**
     * Executes a conditional write and maps condition failures to HTTP 409.
     *
     * @param clientMode selected SDK client
     * @param operation DynamoDB operation
     * @param successStatus HTTP status for success
     * @param syncCall synchronous call
     * @param asyncCall asynchronous call
     * @return operation outcome
     */
    private ResponseEntity<String> conditionalWrite(
            ClientMode clientMode,
            Operation operation,
            int successStatus,
            Supplier<?> syncCall,
            Supplier<? extends CompletableFuture<?>> asyncCall) {
        try {
            if (clientMode == ClientMode.SYNC) {
                syncCall.get();
            } else {
                asyncCall.get().join();
            }
            return outcome(clientMode, operation, true, successStatus, 409);
        } catch (RuntimeException exception) {
            if (hasConditionalCheckFailure(exception)) {
                return outcome(clientMode, operation, false, successStatus, 409);
            }
            throw exception;
        }
    }

    /**
     * Restores the delete scenario so EvoMaster candidates remain independent without database resets.
     *
     * @param clientMode selected SDK client
     */
    private void restoreDeletedItem(ClientMode clientMode) {
        PutItemRequest request = PutItemRequest.builder()
                .tableName(DynamoDbOperationsData.TABLE_NAME)
                .item(DynamoDbOperationsData.item(clientMode, Operation.DELETE_ITEM))
                .build();
        execute(clientMode,
                () -> syncClient.putItem(request),
                () -> asyncClient.putItem(request));
    }

    /**
     * Maps a read result to HTTP 200 or 404.
     *
     * @param clientMode selected SDK client
     * @param operation DynamoDB operation
     * @param found whether a matching item was returned
     * @return operation outcome
     */
    private ResponseEntity<String> readOutcome(ClientMode clientMode, Operation operation, boolean found) {
        return outcome(clientMode, operation, found, 200, 404);
    }

    /**
     * Selects the canonical or missing World Cup shirt number for an outcome request.
     *
     * @param existingPlayer whether the canonical player should be requested
     * @return Lionel Messi's shirt number or a missing fixture number
     */
    private int shirtNumber(boolean existingPlayer) {
        return existingPlayer
                ? DynamoDbOperationsData.TARGET_SHIRT_NUMBER
                : DynamoDbOperationsData.MISSING_SHIRT_NUMBER;
    }

    /**
     * Creates a stable, mode-specific HTTP response marker.
     *
     * @param clientMode selected SDK client
     * @param operation DynamoDB operation
     * @param success whether the operation matched
     * @param successStatus HTTP status for success
     * @param failureStatus HTTP status for failure
     * @return operation outcome
     */
    private ResponseEntity<String> outcome(
            ClientMode clientMode, Operation operation, boolean success, int successStatus, int failureStatus) {
        String marker = clientMode.name() + " " + operation.name() + " " + (success ? "SUCCESS" : "FAILURE");
        return ResponseEntity.status(success ? successStatus : failureStatus).body(marker);
    }

    /**
     * Builds an expression-attribute-name map for one field.
     *
     * @param field DynamoDB field name
     * @return expression name map
     */
    private Map<String, String> nameMap(String field) {
        return Collections.singletonMap("#field", field);
    }

    /**
     * Builds an expression-attribute-value map for one numeric value.
     *
     * @param placeholder DynamoDB expression placeholder
     * @param value numeric value
     * @return expression value map
     */
    private Map<String, AttributeValue> valueMap(String placeholder, int value) {
        Map<String, AttributeValue> values = new HashMap<>();
        values.put(placeholder, AttributeValue.builder().n(Integer.toString(value)).build());
        return values;
    }

    /**
     * Checks an exception chain for a failed DynamoDB condition.
     *
     * @param exception SDK exception
     * @return true when the condition was rejected
     */
    private boolean hasConditionalCheckFailure(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof ConditionalCheckFailedException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
