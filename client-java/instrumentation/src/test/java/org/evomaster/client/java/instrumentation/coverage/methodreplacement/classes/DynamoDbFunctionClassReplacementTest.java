package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import com.foo.somedifferentpackage.examples.dynamodb.EnhancedFunctionApplyExample.Product;
import org.evomaster.client.java.instrumentation.AdditionalInfo;
import org.evomaster.client.java.instrumentation.DynamoDbCommand;
import org.evomaster.client.java.instrumentation.DynamoDbOperationNames;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.extensions.VersionedRecordExtension;
import software.amazon.awssdk.enhanced.dynamodb.internal.operations.DefaultOperationContext;
import software.amazon.awssdk.enhanced.dynamodb.internal.operations.UpdateItemOperation;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies observation of the low-level request passed to an enhanced-client service function.
 */
public class DynamoDbFunctionClassReplacementTest {

    private static final long PRODUCT_VERSION = 3L;

    /**
     * Clears DynamoDB commands before each scenario.
     */
    @BeforeEach
    public void resetTracer() {
        ExecutionTracer.reset();
    }

    /**
     * The version extension contributes a condition to the actual request passed to apply.
     */
    @Test
    public void testRecordVersionConditionFromEnhancedUpdate() {
        Product product = new Product();
        product.setId("football");
        product.setPrice(100);
        product.setVersion(PRODUCT_VERSION);
        TableSchema<Product> schema = TableSchema.fromBean(Product.class);
        UpdateItemEnhancedRequest<Product> enhancedRequest = UpdateItemEnhancedRequest.builder(Product.class)
                .item(product).build();
        UpdateItemRequest request = UpdateItemOperation.create(enhancedRequest).generateRequest(
                schema,
                DefaultOperationContext.create("Products", TableMetadata.primaryIndexName()),
                VersionedRecordExtension.builder().build());
        UpdateItemResponse response = UpdateItemResponse.builder().build();
        Function<Object, Object> call = actual -> {
            assertSame(request, actual);
            return response;
        };

        assertSame(response, DynamoDbFunctionClassReplacement.apply(call, request));

        DynamoDbCommand command = onlyCommand();
        assertSame(request, command.getDdbRequest());
        assertEquals(DynamoDbOperationNames.UPDATE_ITEM, command.getOperationName());
        assertEquals("Products", command.getTableNames().get(0));
        assertTrue(command.isSuccessfullyExecuted());
        assertFalse(request.conditionExpression().isEmpty());
        assertTrue(request.expressionAttributeValues().values().stream()
                .anyMatch(value -> Long.toString(PRODUCT_VERSION).equals(value.n())));
    }

    /**
     * The other eager enhanced operations use the same final-request recording path.
     */
    @Test
    public void testRecordGetPutAndDeleteRequests() {
        List<Object> requests = Arrays.asList(
                GetItemRequest.builder().tableName("WorldCupPlayers").build(),
                PutItemRequest.builder().tableName("WorldCupPlayers").build(),
                DeleteItemRequest.builder().tableName("WorldCupPlayers").build());
        List<DynamoDbOperationNames> operations = Arrays.asList(
                DynamoDbOperationNames.GET_ITEM,
                DynamoDbOperationNames.PUT_ITEM,
                DynamoDbOperationNames.DELETE_ITEM);

        for (int i = 0; i < requests.size(); i++) {
            ExecutionTracer.reset();
            Object request = requests.get(i);
            Object result = new Object();
            assertSame(result, DynamoDbFunctionClassReplacement.apply(
                    (Function<Object, Object>) actual -> result, request));
            DynamoDbCommand command = onlyCommand();
            assertSame(request, command.getDdbRequest());
            assertEquals(operations.get(i), command.getOperationName());
            assertEquals("WorldCupPlayers", command.getTableNames().get(0));
            assertTrue(command.isSuccessfullyExecuted());
        }
    }

    /**
     * A failed call records failure and propagates the original exception.
     */
    @Test
    public void testPreserveSynchronousFailure() {
        UpdateItemRequest request = UpdateItemRequest.builder().tableName("Products").build();
        IllegalStateException failure = new IllegalStateException("Product update failed");
        Function<Object, Object> call = actual -> {
            throw failure;
        };

        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> DynamoDbFunctionClassReplacement.apply(call, request)));
        assertFalse(onlyCommand().isSuccessfullyExecuted());
    }

    /**
     * Asynchronous tracking observes completion without replacing the application's future.
     */
    @Test
    public void testRecordAsynchronousCompletionWithoutWrappingFuture() {
        UpdateItemRequest request = UpdateItemRequest.builder().tableName("Products").build();
        CompletableFuture<UpdateItemResponse> future = new CompletableFuture<>();
        Function<Object, Object> call = actual -> future;

        assertSame(future, DynamoDbFunctionClassReplacement.apply(call, request));
        assertTrue(commands().isEmpty());

        future.completeExceptionally(new IllegalStateException("Version conflict"));
        assertFalse(onlyCommand().isSuccessfullyExecuted());
    }

    /**
     * Non-DynamoDB functions and paginator creation do not represent completed database calls.
     */
    @Test
    public void testLeaveUnrelatedAndLazyCallsUnrecorded() {
        assertEquals("ARGENTINA", DynamoDbFunctionClassReplacement.apply(
                (Function<String, String>) String::toUpperCase, "argentina"));
        QueryRequest request = QueryRequest.builder().tableName("Products").build();
        Object pages = new Object();
        assertSame(pages, DynamoDbFunctionClassReplacement.apply(
                (Function<Object, Object>) actual -> pages, request));
        assertTrue(commands().isEmpty());
    }

    /**
     * Returns the only command recorded by a scenario.
     */
    private DynamoDbCommand onlyCommand() {
        List<DynamoDbCommand> captured = commands();
        assertEquals(1, captured.size());
        return captured.get(0);
    }

    /**
     * Reads commands from the current tracer state.
     */
    private List<DynamoDbCommand> commands() {
        List<AdditionalInfo> entries = ExecutionTracer.exposeAdditionalInfoList();
        assertEquals(1, entries.size());
        Set<DynamoDbCommand> data = entries.get(0).getDynamoDbInfoData();
        return new java.util.ArrayList<>(data);
    }

}
