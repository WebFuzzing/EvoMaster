package org.evomaster.client.java.controller.dynamodb;

import org.evomaster.client.java.controller.dynamodb.operations.*;
import org.evomaster.client.java.controller.dynamodb.operations.comparison.*;
import org.evomaster.client.java.instrumentation.DynamoDbOperationNames;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import software.amazon.awssdk.services.dynamodb.model.*;

import static org.junit.jupiter.api.Assertions.*;


public class DynamoDbRequestParserTest extends DynamoDbTestBase {

    private final DynamoDbRequestParser parser = new DynamoDbRequestParser();

    @Test
    public void testParseByTableGuards() {
        assertTrue(parser.parseByTable(null, DynamoDbOperationNames.QUERY).isEmpty());
        assertTrue(parser.parseByTable(QueryRequest.builder().tableName("players").build(), null).isEmpty());

        QueryRequest request = QueryRequest.builder()
                .tableName("players")
                .keyConditionExpression("id = :id")
                .expressionAttributeValues(attributeValues(":id", stringValue("messi-10")))
                .build();

        QueryOperation operation = parser.parseByTable(request, DynamoDbOperationNames.QUERY).get("players");
        assertComparison(operation, EqualsOperation.class, "id", "messi-10");
    }

    @Test
    public void testQueryParsesKeyAndFilterExpressions() {
        QueryRequest request = QueryRequest.builder()
                .tableName("players")
                .keyConditionExpression("#pk = :id")
                .filterExpression("(age >= :min AND begins_with(#email, :prefix)) OR attribute_not_exists(#deleted)")
                .expressionAttributeNames(names(
                        "#pk", "id",
                        "#email", "email",
                        "#deleted", "deletedAt"
                ))
                .expressionAttributeValues(attributeValues(
                        ":id", stringValue("messi-10"),
                        ":min", numberValue("38"),
                        ":prefix", stringValue("messi@")
                ))
                .build();

        QueryOperation operation = parser.parseByTable(request, DynamoDbOperationNames.QUERY).get("players");
        AndOperation topAnd = castAs(operation, AndOperation.class);
        assertEquals(2, topAnd.getConditions().size());

        assertComparison(topAnd.getConditions().get(0), EqualsOperation.class, "id", "messi-10");

        OrOperation filterOr = castAs(topAnd.getConditions().get(1), OrOperation.class);
        assertEquals(2, filterOr.getConditions().size());

        AndOperation nestedAnd = castAs(filterOr.getConditions().get(0), AndOperation.class);
        assertEquals(2, nestedAnd.getConditions().size());
        assertComparison(nestedAnd.getConditions().get(0), GreaterThanEqualsOperation.class, "age", 38L);

        BeginsWithOperation beginsWith = castAs(nestedAnd.getConditions().get(1), BeginsWithOperation.class);
        assertEquals("email", beginsWith.getFieldName());
        assertEquals("messi@", beginsWith.getPrefix());

        ExistsOperation notExists = castAs(filterOr.getConditions().get(1), ExistsOperation.class);
        assertEquals("deletedAt", notExists.getFieldName());
        assertFalse(notExists.isExists());
    }

    @Test
    public void testQueryParsesLiteralValues() {
        QueryRequest request = QueryRequest.builder()
                .tableName("players")
                .keyConditionExpression("id = 'ronaldo-7'")
                .filterExpression("caps > 1.5e2 AND active = TRUE AND note = NULL")
                .build();

        QueryOperation operation = parser.parseByTable(request, DynamoDbOperationNames.QUERY).get("players");
        AndOperation topAnd = castAs(operation, AndOperation.class);
        assertEquals(2, topAnd.getConditions().size());
        assertComparison(topAnd.getConditions().get(0), EqualsOperation.class, "id", "ronaldo-7");

        AndOperation filterAnd = castAs(topAnd.getConditions().get(1), AndOperation.class);
        assertEquals(3, filterAnd.getConditions().size());

        ComparisonOperation<?> greater = castAs(filterAnd.getConditions().get(0), GreaterThanOperation.class);
        assertEquals("caps", greater.getFieldName());
        assertInstanceOf(Double.class, greater.getValue());
        assertEquals(150.0, (Double) greater.getValue(), 0.000001);

        assertComparison(filterAnd.getConditions().get(1), EqualsOperation.class, "active", true);
        assertComparison(filterAnd.getConditions().get(2), EqualsOperation.class, "note", null);
    }

    @Test
    public void testScanParsesFilterOnly() {
        ScanRequest request = ScanRequest.builder()
                .tableName("players")
                .filterExpression("contains(tags, :tag) AND size(tags) >= :n")
                .expressionAttributeValues(attributeValues(
                        ":tag", stringValue("world-cup"),
                        ":n", numberValue("2")
                ))
                .build();

        QueryOperation operation = parser.parseByTable(request, DynamoDbOperationNames.SCAN).get("players");
        AndOperation and = castAs(operation, AndOperation.class);
        assertEquals(2, and.getConditions().size());

        ContainsOperation contains = castAs(and.getConditions().get(0), ContainsOperation.class);
        assertEquals("tags", contains.getFieldName());
        assertEquals("world-cup", contains.getExpectedValue());

        SizeOperation size = castAs(and.getConditions().get(1), SizeOperation.class);
        assertEquals("tags", size.getFieldName());
        assertEquals(2L, size.getExpectedValue());
    }

    @Test
    public void testScanWithoutFilterReturnsEmptyMap() {
        ScanRequest request = ScanRequest.builder()
                .tableName("players")
                .build();

        assertTrue(parser.parseByTable(request, DynamoDbOperationNames.SCAN).isEmpty());
    }

    @Test
    public void testScanWithBlankTableReturnsEmptyMap() {
        ScanRequest request = ScanRequest.builder()
                .tableName(" ")
                .filterExpression("id = :id")
                .expressionAttributeValues(attributeValues(":id", stringValue("messi-10")))
                .build();

        assertTrue(parser.parseByTable(request, DynamoDbOperationNames.SCAN).isEmpty());
    }

    @Test
    public void testPutItemParsesConditionOnly() {
        PutItemRequest request = PutItemRequest.builder()
                .tableName("players")
                .conditionExpression("attribute_exists(#status) AND #status <> :old")
                .expressionAttributeNames(names("#status", "status"))
                .expressionAttributeValues(attributeValues(":old", stringValue("RETIRED")))
                .build();

        QueryOperation operation = parser.parseByTable(request, DynamoDbOperationNames.PUT_ITEM).get("players");
        AndOperation and = castAs(operation, AndOperation.class);
        assertEquals(2, and.getConditions().size());

        ExistsOperation exists = castAs(and.getConditions().get(0), ExistsOperation.class);
        assertEquals("status", exists.getFieldName());
        assertTrue(exists.isExists());

        assertComparison(and.getConditions().get(1), NotEqualsOperation.class, "status", "RETIRED");
    }

    @Test
    public void testPutItemWithBlankTableReturnsEmptyMap() {
        PutItemRequest request = PutItemRequest.builder()
                .tableName(" ")
                .conditionExpression("id = :id")
                .expressionAttributeValues(attributeValues(":id", stringValue("messi-10")))
                .build();

        assertTrue(parser.parseByTable(request, DynamoDbOperationNames.PUT_ITEM).isEmpty());
    }

    @Test
    public void testDeleteItemCombinesKeyAndCondition() {
        DeleteItemRequest request = DeleteItemRequest.builder()
                .tableName("players")
                .key(attributeValues(
                        "id", stringValue("maradona-10"),
                        "tenant", stringValue("Argentina")
                ))
                .conditionExpression("version = :v")
                .expressionAttributeValues(attributeValues(":v", numberValue("7")))
                .build();

        QueryOperation operation = parser.parseByTable(request, DynamoDbOperationNames.DELETE_ITEM).get("players");
        AndOperation topAnd = castAs(operation, AndOperation.class);
        assertEquals(2, topAnd.getConditions().size());

        AndOperation keyAnd = castAs(topAnd.getConditions().get(0), AndOperation.class);
        assertEquals(2, keyAnd.getConditions().size());
        assertComparison(keyAnd.getConditions().get(0), EqualsOperation.class, "id", "maradona-10");
        assertComparison(keyAnd.getConditions().get(1), EqualsOperation.class, "tenant", "Argentina");

        assertComparison(topAnd.getConditions().get(1), EqualsOperation.class, "version", 7L);
    }

    @Test
    public void testUpdateItemCombinesKeyAndCondition() {
        UpdateItemRequest request = UpdateItemRequest.builder()
                .tableName("players")
                .key(attributeValues("id", stringValue("ronaldo-7")))
                .conditionExpression("#age BETWEEN :l AND :u")
                .expressionAttributeNames(names("#age", "age"))
                .expressionAttributeValues(attributeValues(
                        ":l", numberValue("38"),
                        ":u", numberValue("41")
                ))
                .build();

        QueryOperation operation = parser.parseByTable(request, DynamoDbOperationNames.UPDATE_ITEM).get("players");
        AndOperation and = castAs(operation, AndOperation.class);
        assertEquals(2, and.getConditions().size());

        assertComparison(and.getConditions().get(0), EqualsOperation.class, "id", "ronaldo-7");
        BetweenOperation between = castAs(and.getConditions().get(1), BetweenOperation.class);
        assertEquals("age", between.getFieldName());
        assertEquals(38L, between.getLowerBound());
        assertEquals(41L, between.getUpperBound());
    }

    @Test
    public void testGetItemParsesCompositeKey() {
        GetItemRequest request = GetItemRequest.builder()
                .tableName("players")
                .key(attributeValues(
                        "id", stringValue("pele-10"),
                        "tenant", stringValue("brazil")
                ))
                .build();

        QueryOperation operation = parser.parseByTable(request, DynamoDbOperationNames.GET_ITEM).get("players");
        AndOperation and = castAs(operation, AndOperation.class);
        assertEquals(2, and.getConditions().size());
        assertComparison(and.getConditions().get(0), EqualsOperation.class, "id", "pele-10");
        assertComparison(and.getConditions().get(1), EqualsOperation.class, "tenant", "brazil");
    }

    @Test
    public void testGetItemWithoutKeyReturnsEmptyMap() {
        GetItemRequest request = GetItemRequest.builder()
                .tableName("players")
                .build();

        assertTrue(parser.parseByTable(request, DynamoDbOperationNames.GET_ITEM).isEmpty());
    }

    @Test
    public void testBatchGetParsesEachTableAndSkipsInvalidTableNames() {
        Map<String, KeysAndAttributes> requestItems = new LinkedHashMap<>();
        requestItems.put("players", KeysAndAttributes.builder().keys(Arrays.asList(
                attributeValues("id", stringValue("messi-10")),
                attributeValues("id", stringValue("ronaldo-7"))
        )).build());
        requestItems.put("matches", KeysAndAttributes.builder().keys(Collections.singletonList(
                attributeValues(
                        "matchId", stringValue("wc-final-1986"),
                        "tenant", stringValue("Mexico")
                )
        )).build());
        requestItems.put("", KeysAndAttributes.builder().keys(Collections.singletonList(
                attributeValues("id", stringValue("pele-10"))
        )).build());

        BatchGetItemRequest request = BatchGetItemRequest.builder()
                .requestItems(requestItems)
                .build();

        Map<String, QueryOperation> parsed = parser.parseByTable(request, DynamoDbOperationNames.BATCH_GET_ITEM);
        assertEquals(2, parsed.size());

        OrOperation players = castAs(parsed.get("players"), OrOperation.class);
        assertEquals(2, players.getConditions().size());
        assertComparison(players.getConditions().get(0), EqualsOperation.class, "id", "messi-10");
        assertComparison(players.getConditions().get(1), EqualsOperation.class, "id", "ronaldo-7");

        AndOperation matches = castAs(parsed.get("matches"), AndOperation.class);
        assertEquals(2, matches.getConditions().size());
        assertComparison(matches.getConditions().get(0), EqualsOperation.class, "matchId", "wc-final-1986");
        assertComparison(matches.getConditions().get(1), EqualsOperation.class, "tenant", "Mexico");
    }

    @Test
    public void testBatchGetWithEmptyKeysDoesNotAddTable() {
        BatchGetItemRequest request = BatchGetItemRequest.builder()
                .requestItems(Collections.singletonMap(
                        "players",
                        KeysAndAttributes.builder().keys(Collections.singletonList(
                                Collections.emptyMap()
                        )).build()
                ))
                .build();

        assertTrue(parser.parseByTable(request, DynamoDbOperationNames.BATCH_GET_ITEM).isEmpty());
    }

    @Test
    public void testBatchGetSkipsInvalidRequestItemsShapes() {
        assertTrue(parser.parseByTable(new RequestWithNonMapRequestItems(), DynamoDbOperationNames.BATCH_GET_ITEM).isEmpty());
        assertTrue(parser.parseByTable(new RequestWithNonCollectionKeys(), DynamoDbOperationNames.BATCH_GET_ITEM).isEmpty());
    }

    @Test
    public void testConditionWithTypeAndInParsing() {
        PutItemRequest request = PutItemRequest.builder()
                .tableName("players")
                .conditionExpression("attribute_type(kind, S) AND status IN (:s1, 'GOAT', champion)")
                .expressionAttributeValues(attributeValues(":s1", stringValue("LEGEND")))
                .build();

        QueryOperation operation = parser.parseByTable(request, DynamoDbOperationNames.PUT_ITEM).get("players");
        AndOperation and = castAs(operation, AndOperation.class);
        assertEquals(2, and.getConditions().size());

        TypeOperation type = castAs(and.getConditions().get(0), TypeOperation.class);
        assertEquals("kind", type.getFieldName());
        assertEquals("S", type.getExpectedType());

        InOperation<?> in = castAs(and.getConditions().get(1), InOperation.class);
        assertEquals("status", in.getFieldName());
        assertEquals(Arrays.asList("LEGEND", "GOAT", "champion"), in.getValues());
    }

    @Test
    public void testBlankTableNameReturnsEmptyMap() {
        QueryRequest request = QueryRequest.builder()
                .tableName(" ")
                .keyConditionExpression("id = :id")
                .expressionAttributeValues(attributeValues(":id", stringValue("messi-10")))
                .build();

        assertTrue(parser.parseByTable(request, DynamoDbOperationNames.QUERY).isEmpty());
    }

    private static class RequestWithNonMapRequestItems {
        @SuppressWarnings("unused") // will be invoked by reflection
        public Object requestItems() {
            return "not-a-map";
        }
    }

    //Reflection will invoke fake class, method
    private static class RequestWithNonCollectionKeys {
        @SuppressWarnings("unused") // will be invoked by reflection
        public Map<String, Object> requestItems() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("players", new NonCollectionKeysHolder());
            return map;
        }
    }

    //Reflection will invoke fake class, method
    private static class NonCollectionKeysHolder {
        @SuppressWarnings("unused") // will be invoked by reflection
        public Object keys() {
            return "not-a-collection";
        }
    }
}
