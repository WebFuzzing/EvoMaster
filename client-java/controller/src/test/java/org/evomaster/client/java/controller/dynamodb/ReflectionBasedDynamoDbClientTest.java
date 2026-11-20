package org.evomaster.client.java.controller.dynamodb;

import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbAttributeValueDto;
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbDtoUtils;
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbInsertionDto;
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbInsertionResultsDto;
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbKeyScalarTypeDto;
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbValueTypeDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.math.BigDecimal;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Checks that {@link ReflectionBasedDynamoDbClient} finds and calls the DynamoDB client's
 * methods through reflection without compile-time coupling to concrete client types.
 */
public class ReflectionBasedDynamoDbClientTest {

    private static final String TABLE = "WorldCupPlayers";

    @Test
    public void testRejectsNullClient() {
        assertThrows(NullPointerException.class, () -> new ReflectionBasedDynamoDbClient(null));
    }

    @Test
    public void testExecuteInsertionsSynchronousClient() {
        DynamoDbClient mockClient = mock(DynamoDbClient.class);
        when(mockClient.putItem(any(PutItemRequest.class))).thenReturn(PutItemResponse.builder().build());

        ReflectionBasedDynamoDbClient client = new ReflectionBasedDynamoDbClient(mockClient);
        DynamoDbInsertionResultsDto results = client.executeInsertions(Collections.singletonList(worldCupPlayer()));

        ArgumentCaptor<PutItemRequest> captor = ArgumentCaptor.forClass(PutItemRequest.class);
        verify(mockClient).putItem(captor.capture());
        PutItemRequest request = captor.getValue();
        assertEquals(TABLE, request.tableName());
        assertEquals("Argentina", request.item().get("country").s());
        assertEquals("10", request.item().get("fifaId").n());
        assertTrue(request.item().get("captain").bool());
        assertEquals(Collections.singletonList(true), results.executionResults);
        assertNull(results.failedInsertionIndex);
    }

    @Test
    public void testExecuteInsertionsAsynchronousClient() {
        DynamoDbAsyncClient mockClient = mock(DynamoDbAsyncClient.class);
        when(mockClient.putItem(any(PutItemRequest.class))).thenReturn(
                CompletableFuture.completedFuture(PutItemResponse.builder().build()));

        ReflectionBasedDynamoDbClient client = new ReflectionBasedDynamoDbClient(mockClient);
        DynamoDbInsertionResultsDto results = client.executeInsertions(Collections.singletonList(worldCupPlayer()));

        verify(mockClient).putItem(any(PutItemRequest.class));
        assertEquals(Collections.singletonList(true), results.executionResults);
    }

    @Test
    public void testExecuteInsertionsPartialFailure() {
        DynamoDbClient mockClient = mock(DynamoDbClient.class);
        when(mockClient.putItem(any(PutItemRequest.class)))
                .thenReturn(PutItemResponse.builder().build())
                .thenThrow(new IllegalStateException("DynamoDB service error"));

        ReflectionBasedDynamoDbClient client = new ReflectionBasedDynamoDbClient(mockClient);
        DynamoDbInsertionResultsDto results = client.executeInsertions(
                Arrays.asList(worldCupPlayer(), worldCupPlayer(), worldCupPlayer()));

        assertEquals(Arrays.asList(true, false, false), results.executionResults);
        assertEquals(Integer.valueOf(1), results.failedInsertionIndex);
        verify(mockClient, times(2)).putItem(any(PutItemRequest.class));
    }

    @Test
    public void testExecuteInsertionsRejectsMissingInsertions() {
        DynamoDbClient mockClient = mock(DynamoDbClient.class);
        ReflectionBasedDynamoDbClient client = new ReflectionBasedDynamoDbClient(mockClient);

        assertThrows(NullPointerException.class, () -> client.executeInsertions(null));
        assertThrows(IllegalArgumentException.class, () -> client.executeInsertions(Collections.emptyList()));
        verify(mockClient, times(0)).putItem(any(PutItemRequest.class));
    }

    @Test
    public void testExecuteInsertionsRejectsClientWithoutPutItemMethod() {
        ReflectionBasedDynamoDbClient client = new ReflectionBasedDynamoDbClient(new Object());
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> client.executeOne(worldCupPlayer()));
        assertEquals("The provided client does not expose putItem(PutItemRequest)", exception.getMessage());

        DynamoDbInsertionResultsDto results = client.executeInsertions(Collections.singletonList(worldCupPlayer()));
        assertEquals(Collections.singletonList(false), results.executionResults);
        assertEquals(Integer.valueOf(0), results.failedInsertionIndex);
    }

    @Test
    public void testGetItemsWithPagination() {
        DynamoDbClient mockClient = mock(DynamoDbClient.class);
        Map<String, AttributeValue> nextKey = Collections.singletonMap(
                "country", AttributeValue.builder().s("Argentina").build());

        Map<String, AttributeValue> item1 = new LinkedHashMap<>();
        item1.put("playerName", AttributeValue.builder().s("Diego Maradona").build());
        item1.put("fifaId", AttributeValue.builder().n("190042").build());

        Map<String, AttributeValue> item2 = new LinkedHashMap<>();
        item2.put("playerName", AttributeValue.builder().s("Lionel Messi").build());
        item2.put("fifaId", AttributeValue.builder().n("158023").build());

        ScanResponse page1 = ScanResponse.builder()
                .items(Collections.singletonList(item1))
                .lastEvaluatedKey(nextKey)
                .build();
        ScanResponse page2 = ScanResponse.builder()
                .items(Collections.singletonList(item2))
                .lastEvaluatedKey(Collections.emptyMap())
                .build();

        when(mockClient.scan(any(ScanRequest.class))).thenReturn(page1, page2);

        ReflectionBasedDynamoDbClient client = new ReflectionBasedDynamoDbClient(mockClient);
        List<Map<String, Object>> items = client.getItems(TABLE);

        assertEquals(2, items.size());
        assertEquals("Diego Maradona", items.get(0).get("playerName"));
        assertEquals("Lionel Messi", items.get(1).get("playerName"));
        verify(mockClient, times(2)).scan(any(ScanRequest.class));
    }

    @Test
    public void testGetItemsAsynchronousClient() {
        DynamoDbAsyncClient mockClient = mock(DynamoDbAsyncClient.class);
        Map<String, AttributeValue> item = new LinkedHashMap<>();
        item.put("country", AttributeValue.builder().s("Argentina").build());
        item.put("playerName", AttributeValue.builder().s("Lionel Messi").build());

        ScanResponse response = ScanResponse.builder()
                .items(Collections.singletonList(item))
                .build();
        when(mockClient.scan(any(ScanRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(response));

        ReflectionBasedDynamoDbClient client = new ReflectionBasedDynamoDbClient(mockClient);
        List<Map<String, Object>> items = client.getItems(TABLE);

        assertEquals(1, items.size());
        assertEquals("Argentina", items.get(0).get("country"));
        assertEquals("Lionel Messi", items.get(0).get("playerName"));
        verify(mockClient).scan(any(ScanRequest.class));
    }

    @Test
    public void testGetItemsRejectsRepeatedPaginationKey() {
        DynamoDbClient mockClient = mock(DynamoDbClient.class);
        Map<String, AttributeValue> nextKey = Collections.singletonMap(
                "country", AttributeValue.builder().s("Argentina").build());
        Map<String, AttributeValue> item = Collections.singletonMap(
                "playerName", AttributeValue.builder().s("Lionel Messi").build());
        ScanResponse response = ScanResponse.builder()
                .items(Collections.singletonList(item))
                .lastEvaluatedKey(nextKey)
                .build();
        when(mockClient.scan(any(ScanRequest.class))).thenReturn(response);

        ReflectionBasedDynamoDbClient client = new ReflectionBasedDynamoDbClient(mockClient);
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> client.getItems(TABLE));

        assertEquals("DynamoDB scan returned the same pagination key twice", exception.getMessage());
        verify(mockClient, times(2)).scan(any(ScanRequest.class));
    }

    @Test
    public void testGetItemsReportsUnderlyingScanFailure() {
        IllegalStateException scanFailure;
        RuntimeException exception;
        try (DynamoDbClient mockClient = mock(DynamoDbClient.class)) {
            scanFailure = new IllegalStateException("World Cup table unavailable");
            when(mockClient.scan(any(ScanRequest.class))).thenThrow(scanFailure);

            ReflectionBasedDynamoDbClient client = new ReflectionBasedDynamoDbClient(mockClient);
            exception = assertThrows(RuntimeException.class,
                    () -> client.getItems(TABLE));
        }

        assertSame(scanFailure, exception.getCause());
        assertTrue(exception.getMessage().contains(TABLE));
        assertTrue(exception.getMessage().contains("World Cup table unavailable"));
    }

    @Test
    public void testGetItemsRejectsNullTableName() {
        DynamoDbClient mockClient = mock(DynamoDbClient.class);
        ReflectionBasedDynamoDbClient client = new ReflectionBasedDynamoDbClient(mockClient);

        NullPointerException exception = assertThrows(NullPointerException.class,
                () -> client.getItems(null));
        assertEquals("DynamoDB table name cannot be null", exception.getMessage());
    }

    @Test
    public void testGetItemsRejectsClientWithoutScanMethod() {
        ReflectionBasedDynamoDbClient client = new ReflectionBasedDynamoDbClient(new Object());
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> client.getItems(TABLE));

        assertEquals("The provided client does not expose scan(ScanRequest)", exception.getMessage());
    }


    /** Nested template and set values must retain their native DynamoDB types. */
    @Test
    public void testExecuteInsertionsWithDocumentAndSetValues() {
        DynamoDbClient mockClient = mock(DynamoDbClient.class);
        when(mockClient.putItem(any(PutItemRequest.class))).thenReturn(PutItemResponse.builder().build());

        DynamoDbInsertionDto insertion = worldCupPlayer();
        Map<String, Object> document = new LinkedHashMap<>();
        Map<String, Object> player = new LinkedHashMap<>();
        player.put("name", "Lionel Messi");
        player.put("captain", true);
        document.put("players", Collections.singletonList(player));
        document.put("score", new BigDecimal("10.1234567890123456789"));
        document.put("coach", null);
        document.put("emptyMap", Collections.emptyMap());
        document.put("emptyList", Collections.emptyList());

        insertion.attributes.add(DynamoDbDtoUtils.fromPlainValue("squad", document));
        insertion.attributes.add(DynamoDbDtoUtils.fromPlainValue("positions",
                new LinkedHashSet<>(Arrays.asList("forward", "captain"))));
        insertion.attributes.add(DynamoDbDtoUtils.fromPlainValue("jerseyNumbers",
                new LinkedHashSet<>(Arrays.asList(10, new BigDecimal("30.5")))));

        ReflectionBasedDynamoDbClient client = new ReflectionBasedDynamoDbClient(mockClient);
        DynamoDbInsertionResultsDto results = client.executeInsertions(Collections.singletonList(insertion));

        assertTrue(results.executionResults.get(0));
        ArgumentCaptor<PutItemRequest> captor = ArgumentCaptor.forClass(PutItemRequest.class);
        verify(mockClient).putItem(captor.capture());
        AttributeValue squad = captor.getValue().item().get("squad");
        assertNotNull(squad);
        assertTrue(squad.hasM());
        assertEquals(true, squad.m().get("coach").nul());
        assertEquals("10.1234567890123456789", squad.m().get("score").n());
        assertEquals("Lionel Messi", squad.m().get("players").l().get(0).m().get("name").s());
        AttributeValue positions = captor.getValue().item().get("positions");
        assertTrue(positions.hasSs());
        assertEquals(Arrays.asList("captain", "forward"), positions.ss());
        AttributeValue jerseyNumbers = captor.getValue().item().get("jerseyNumbers");
        assertTrue(jerseyNumbers.hasNs());
        assertEquals(Arrays.asList("10", "30.5"), jerseyNumbers.ns());
    }

    @Test
    public void testExecuteInsertionsRejectsEmptySetAttribute() {
        DynamoDbClient mockClient = mock(DynamoDbClient.class);
        DynamoDbInsertionDto insertion = worldCupPlayer();
        DynamoDbAttributeValueDto emptySet = new DynamoDbAttributeValueDto();
        emptySet.attributeName = "positions";
        emptySet.type = DynamoDbValueTypeDto.STRING_SET;
        emptySet.setValue = Collections.emptyList();
        insertion.attributes.add(emptySet);

        ReflectionBasedDynamoDbClient client = new ReflectionBasedDynamoDbClient(mockClient);
        DynamoDbInsertionResultsDto results = client.executeInsertions(Collections.singletonList(insertion));
        assertEquals(Collections.singletonList(false), results.executionResults);
        assertEquals(Integer.valueOf(0), results.failedInsertionIndex);
        verify(mockClient, times(0)).putItem(any(PutItemRequest.class));
    }

    /**
     * Simulates an AWS SDK client whose model classes are shaded into a relocated package
     * (e.g., shaded.emb.software.amazon.awssdk.services.dynamodb.model).
     */
    @Test
    public void testSupportsRelocatedModelPackageForScanAndPut(@TempDir Path tempDir) throws Exception {
        String pkg = "shaded.emb.software.amazon.awssdk.services.dynamodb.model";
        Path pkgDir = tempDir.resolve("shaded/emb/software/amazon/awssdk/services/dynamodb/model");
        Files.createDirectories(pkgDir);

        Map<String, String> sources = new LinkedHashMap<>();
        sources.put("AttributeValue.java",
                "package " + pkg + ";\n"
                        + "import java.util.*;\n"
                        + "public class AttributeValue {\n"
                        + "  private String s, n; private Boolean bool, nul;\n"
                        + "  public static Builder builder() { return new Builder(); }\n"
                        + "  public String s() { return s; }\n"
                        + "  public String n() { return n; }\n"
                        + "  public Boolean bool() { return bool; }\n"
                        + "  public Boolean nul() { return nul; }\n"
                        + "  public boolean hasM() { return false; }\n"
                        + "  public Map<String, AttributeValue> m() { return null; }\n"
                        + "  public boolean hasL() { return false; }\n"
                        + "  public List<AttributeValue> l() { return null; }\n"
                        + "  public static class Builder {\n"
                        + "    private final AttributeValue v = new AttributeValue();\n"
                        + "    public Builder s(String s) { v.s = s; return this; }\n"
                        + "    public Builder n(String n) { v.n = n; return this; }\n"
                        + "    public Builder bool(Boolean b) { v.bool = b; return this; }\n"
                        + "    public Builder nul(Boolean nul) { v.nul = nul; return this; }\n"
                        + "    public AttributeValue build() { return v; }\n"
                        + "  }\n"
                        + "}\n");
        sources.put("PutItemRequest.java",
                "package " + pkg + ";\n"
                        + "import java.util.*;\n"
                        + "public class PutItemRequest {\n"
                        + "  public final Map<String, AttributeValue> item;\n"
                        + "  public PutItemRequest(Map<String, AttributeValue> item) { this.item = item; }\n"
                        + "  public static Builder builder() { return new Builder(); }\n"
                        + "  public static class Builder {\n"
                        + "    private Map<String, AttributeValue> item;\n"
                        + "    public Builder tableName(String t) { return this; }\n"
                        + "    public Builder item(Map<String, AttributeValue> i) { this.item = i; return this; }\n"
                        + "    public PutItemRequest build() { return new PutItemRequest(item); }\n"
                        + "  }\n"
                        + "}\n");
        sources.put("ScanRequest.java",
                "package " + pkg + ";\n"
                        + "import java.util.*;\n"
                        + "public class ScanRequest {\n"
                        + "  public static Builder builder() { return new Builder(); }\n"
                        + "  public static class Builder {\n"
                        + "    public Builder tableName(String t) { return this; }\n"
                        + "    public Builder exclusiveStartKey(Map<?, ?> k) { return this; }\n"
                        + "    public ScanRequest build() { return new ScanRequest(); }\n"
                        + "  }\n"
                        + "}\n");
        sources.put("ScanResponse.java",
                "package " + pkg + ";\n"
                        + "import java.util.*;\n"
                        + "public class ScanResponse {\n"
                        + "  private final List<Map<String, AttributeValue>> items;\n"
                        + "  public ScanResponse(List<Map<String, AttributeValue>> items) { this.items = items; }\n"
                        + "  public List<Map<String, AttributeValue>> items() { return items; }\n"
                        + "  public Map<?, ?> lastEvaluatedKey() { return Collections.emptyMap(); }\n"
                        + "}\n");
        sources.put("FakeRelocatedClient.java",
                "package " + pkg + ";\n"
                        + "import java.util.*;\n"
                        + "public class FakeRelocatedClient {\n"
                        + "  public final List<Map<String, AttributeValue>> items = new ArrayList<>();\n"
                        + "  public Object putItem(PutItemRequest req) { items.add(req.item); return new Object(); }\n"
                        + "  public ScanResponse scan(ScanRequest req) { return new ScanResponse(items); }\n"
                        + "}\n");

        List<String> filesToCompile = new ArrayList<>();
        for (Map.Entry<String, String> entry : sources.entrySet()) {
            Path src = pkgDir.resolve(entry.getKey());
            Files.write(src, entry.getValue().getBytes(StandardCharsets.UTF_8));
            filesToCompile.add(src.toString());
        }

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler);
        int exit = compiler.run(null, null, null, filesToCompile.toArray(new String[0]));
        assertEquals(0, exit);

        try (URLClassLoader relocatedLoader = new URLClassLoader(
                new URL[]{tempDir.toUri().toURL()},
                ReflectionBasedDynamoDbClient.class.getClassLoader())) {
            Object relocatedClient = Class.forName(pkg + ".FakeRelocatedClient", true, relocatedLoader)
                    .getDeclaredConstructor().newInstance();
            ReflectionBasedDynamoDbClient client = new ReflectionBasedDynamoDbClient(relocatedClient);

            DynamoDbInsertionResultsDto single = client.executeInsertions(Collections.singletonList(worldCupPlayer()));
            assertEquals(Collections.singletonList(true), single.executionResults);

            List<Map<String, Object>> items = client.getItems(TABLE);
            assertEquals(1, items.size());
            assertEquals("Argentina", items.get(0).get("country"));
            assertEquals(10L, items.get(0).get("fifaId"));
            assertEquals(true, items.get(0).get("captain"));
        }
    }

    private static DynamoDbInsertionDto worldCupPlayer() {
        DynamoDbInsertionDto dto = new DynamoDbInsertionDto();
        dto.tableName = TABLE;
        dto.attributes = new ArrayList<>();
        dto.attributes.add(new DynamoDbAttributeValueDto("country", DynamoDbKeyScalarTypeDto.STRING, "Argentina"));
        dto.attributes.add(new DynamoDbAttributeValueDto("fifaId", DynamoDbKeyScalarTypeDto.NUMBER, "10"));
        dto.attributes.add(new DynamoDbAttributeValueDto("captain", DynamoDbKeyScalarTypeDto.BOOLEAN, "true"));
        return dto;
    }
}
