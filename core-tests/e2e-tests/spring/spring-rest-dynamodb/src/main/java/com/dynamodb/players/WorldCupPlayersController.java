package com.dynamodb.players;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

import java.util.HashMap;
import java.util.Map;

/**
 * REST endpoint backed by a DynamoDB query with a conditional filter.
 */
@RestController
@RequestMapping("/players")
public class WorldCupPlayersController {

    private static final String TABLE_NAME = "WorldCupPlayers";

    private final DynamoDbAsyncClient dynamoDbClient;

    /**
     * Creates a controller backed by the given DynamoDB client.
     *
     * @param dynamoDbClient DynamoDB client
     */
    public WorldCupPlayersController(DynamoDbAsyncClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
    }

    /**
     * Finds an Argentinian World Cup player by exact FIFA identifier.
     *
     * @param fifaId requested FIFA identifier
     * @return 200 when the filter matches, otherwise 404
     */
    @GetMapping("/{fifaId}")
    public ResponseEntity<String> findPlayer(@PathVariable long fifaId) {
        Map<String, String> attributeNames = new HashMap<>();
        attributeNames.put("#country", "country");
        attributeNames.put("#fifaId", "fifaId");

        Map<String, AttributeValue> attributeValues = new HashMap<>();
        attributeValues.put(":country", AttributeValue.builder().s("Argentina").build());
        attributeValues.put(":fifaId", AttributeValue.builder().n(Long.toString(fifaId)).build());

        QueryRequest request = QueryRequest.builder()
                .tableName(TABLE_NAME)
                .keyConditionExpression("#country = :country")
                .filterExpression("#fifaId = :fifaId")
                .expressionAttributeNames(attributeNames)
                .expressionAttributeValues(attributeValues)
                .build();

        QueryResponse response = dynamoDbClient.query(request).join();
        if (response.items().isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok("Lionel Messi plays for Argentina");
    }
}
