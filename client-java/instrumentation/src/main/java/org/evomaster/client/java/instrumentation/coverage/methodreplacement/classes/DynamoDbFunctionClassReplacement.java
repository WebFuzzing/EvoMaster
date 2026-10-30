package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.instrumentation.dynamodb.DynamoDbOperationNames;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.MethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ReplacementCallSites;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.UsageFilter;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses.DynamoDbClassReplacement;
import org.evomaster.client.java.instrumentation.shared.ReplacementCategory;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Observes enhanced-client service functions after the SDK has built the low-level request.
 */
public class DynamoDbFunctionClassReplacement implements MethodReplacementClass {

    private static final String REQUEST_PACKAGE = "software.amazon.awssdk.services.dynamodb.model.";

    /**
     * @return the functional interface whose apply call is replaced
     */
    @Override
    public Class<?> getTargetClass() {
        return Function.class;
    }

    /**
     * Executes the service function once and records the completed DynamoDB operation.
     *
     * @param caller service function created by the enhanced client
     * @param request final low-level request
     * @return the original function result
     */
    @ReplacementCallSites({
            "software.amazon.awssdk.enhanced.dynamodb.internal.operations.CommonOperation",
            "software.amazon.awssdk.enhanced.dynamodb.internal.operations.DatabaseOperation"
    })
    @Replacement(type = ReplacementType.TRACKER, category = ReplacementCategory.DYNAMODB,
            usageFilter = UsageFilter.ONLY_THIRD_PARTY, isPure = false)
    public static Object apply(Function caller, Object request) {
        DynamoDbOperationNames operation = operationFor(request);
        if (operation == null) {
            return caller.apply(request);
        }

        long start = System.currentTimeMillis();
        try {
            Object result = caller.apply(request);
            if (result instanceof CompletableFuture) {
                ((CompletableFuture<?>) result).whenComplete((value, failure) ->
                        DynamoDbClassReplacement.recordDynamoDbTraceInfo(request, start, operation, failure == null));
            } else {
                DynamoDbClassReplacement.recordDynamoDbTraceInfo(request, start, operation, true);
            }
            return result;
        } catch (RuntimeException | Error failure) {
            DynamoDbClassReplacement.recordDynamoDbTraceInfo(request, start, operation, false);
            throw failure;
        }
    }

    /**
     * Returns the tracked operation for an eager low-level request.
     */
    private static DynamoDbOperationNames operationFor(Object request) {
        if (request == null) {
            return null;
        }
        switch (request.getClass().getName()) {
            case REQUEST_PACKAGE + "GetItemRequest":
                return DynamoDbOperationNames.GET_ITEM;
            case REQUEST_PACKAGE + "PutItemRequest":
                return DynamoDbOperationNames.PUT_ITEM;
            case REQUEST_PACKAGE + "UpdateItemRequest":
                return DynamoDbOperationNames.UPDATE_ITEM;
            case REQUEST_PACKAGE + "DeleteItemRequest":
                return DynamoDbOperationNames.DELETE_ITEM;
            default:
                return null;
        }
    }
}
