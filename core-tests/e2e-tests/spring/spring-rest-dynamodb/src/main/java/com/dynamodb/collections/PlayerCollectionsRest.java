package com.dynamodb.collections;

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
import java.util.List;
import java.util.Map;

/**
 * REST endpoint backed by DynamoDB items containing nested maps, lists, string sets, number sets, and nulls.
 */
@RestController
@RequestMapping("/collections/players")
public class PlayerCollectionsRest {

    public static final String TABLE_NAME = "PlayerCollections";
    public static final String SEED_COUNTRY = "Argentina";

    private final DynamoDbAsyncClient dynamoDbClient;

    public PlayerCollectionsRest(DynamoDbAsyncClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
    }

    /**
     * Reads a non-seed country item and verifies that nested document, collection, set, and null
     * DynamoDB attribute types are intact.
     *
     * @param country requested country key
     * @return 200 when a non-seed item is present with valid collection/set/null structure,
     * 409 for the seed item, 404 when absent, or 500 if any attribute type is malformed
     */
    @GetMapping("/{country}")
    public ResponseEntity<String> findByCountry(@PathVariable String country) {
        if (SEED_COUNTRY.equals(country)) {
            return ResponseEntity.status(409).body("SEED_COUNTRY_EXCLUDED");
        }

        Map<String, String> attributeNames = new HashMap<>();
        attributeNames.put("#country", "country");

        Map<String, AttributeValue> attributeValues = new HashMap<>();
        attributeValues.put(":country", AttributeValue.builder().s(country).build());

        QueryRequest request = QueryRequest.builder()
                .tableName(TABLE_NAME)
                .consistentRead(true)
                .keyConditionExpression("#country = :country")
                .expressionAttributeNames(attributeNames)
                .expressionAttributeValues(attributeValues)
                .build();

        QueryResponse response = dynamoDbClient.query(request).join();
        if (response.items().isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Map<String, AttributeValue> item = response.items().get(0);
        if (!hasValidCollectionAttributes(item)) {
            return ResponseEntity.internalServerError().body("INVALID_COLLECTION_ATTRIBUTES");
        }

        return ResponseEntity.ok("COLLECTION_TEMPLATE_MATCHED");
    }

    private static boolean hasValidCollectionAttributes(Map<String, AttributeValue> item) {
        AttributeValue profile = item.get("profile");
        if (profile == null || profile.type() != AttributeValue.Type.M) {
            return false;
        }
        Map<String, AttributeValue> profileMap = profile.m();
        AttributeValue captain = profileMap.get("captain");
        AttributeValue caps = profileMap.get("caps");
        AttributeValue verified = profileMap.get("verified");
        AttributeValue club = profileMap.get("club");
        if (captain == null || captain.type() != AttributeValue.Type.S
                || caps == null || caps.type() != AttributeValue.Type.N
                || verified == null || verified.type() != AttributeValue.Type.BOOL
                || club == null || club.type() != AttributeValue.Type.M) {
            return false;
        }
        AttributeValue city = club.m().get("city");
        if (city == null || city.type() != AttributeValue.Type.S) {
            return false;
        }

        AttributeValue tags = item.get("tags");
        if (tags == null || tags.type() != AttributeValue.Type.L || tags.l().size() != 6) {
            return false;
        }
        List<AttributeValue> tagList = tags.l();
        if (tagList.get(0).type() != AttributeValue.Type.S
                || tagList.get(1).type() != AttributeValue.Type.M
                || tagList.get(1).m().get("tier") == null
                || tagList.get(1).m().get("tier").type() != AttributeValue.Type.S
                || tagList.get(2).type() != AttributeValue.Type.L
                || tagList.get(2).l().size() != 1
                || tagList.get(2).l().get(0).type() != AttributeValue.Type.S
                || tagList.get(3).type() != AttributeValue.Type.SS
                || tagList.get(3).ss().isEmpty()
                || tagList.get(4).type() != AttributeValue.Type.NS
                || tagList.get(4).ns().isEmpty()
                || tagList.get(5).type() != AttributeValue.Type.NUL
                || !Boolean.TRUE.equals(tagList.get(5).nul())) {
            return false;
        }

        AttributeValue roles = item.get("roles");
        AttributeValue jerseyNumbers = item.get("jerseyNumbers");
        AttributeValue retiredAt = item.get("retiredAt");
        AttributeValue emptyTags = item.get("emptyTags");

        return roles != null && roles.type() == AttributeValue.Type.SS && !roles.ss().isEmpty()
                && jerseyNumbers != null && jerseyNumbers.type() == AttributeValue.Type.NS && !jerseyNumbers.ns().isEmpty()
                && retiredAt != null && retiredAt.type() == AttributeValue.Type.NUL && Boolean.TRUE.equals(retiredAt.nul())
                && emptyTags != null && emptyTags.type() == AttributeValue.Type.L && emptyTags.l().isEmpty();
    }
}
