package org.evomaster.e2etests.spring.rest.dynamodb;

import com.dynamodb.operations.DynamoDbOperationsData.ClientMode;
import com.dynamodb.operations.DynamoDbOperationsData.Operation;
import com.foo.spring.rest.dynamodb.DynamoDbOperationsController;
import org.evomaster.core.problem.rest.data.HttpVerb;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class DynamoDbOperationsEMTest extends DynamoDbTestBase {

    @BeforeAll
    public static void initClass() throws Exception {
        initDynamoDbTest(new DynamoDbOperationsController());
    }

    @Test
    public void testAllDynamoDbOperations() throws Throwable {
        runTestHandlingFlakyAndCompilation(
                "DynamoDbOperationsEM",
                "org.foo.spring.rest.dynamodb.DynamoDbOperationsEM",
                1000,
                true,
                args -> {
                    configureDynamoDbHeuristics(args, true);
                    setOption(args, "maxTestSize", "1");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertFalse(solution.getIndividuals().isEmpty());
                    for (ClientMode clientMode : ClientMode.values()) {
                        assertOperation(solution, clientMode, Operation.GET_ITEM,
                                HttpVerb.GET, "get-item", 200, 404);
                        assertOperation(solution, clientMode, Operation.BATCH_GET_ITEM,
                                HttpVerb.GET, "batch-get-item", 200, 404);
                        assertOperation(solution, clientMode, Operation.PUT_ITEM,
                                HttpVerb.POST, "put-item", 201, 409);
                        assertOperation(solution, clientMode, Operation.UPDATE_ITEM,
                                HttpVerb.PUT, "update-item", 200, 409);
                        assertOperation(solution, clientMode, Operation.DELETE_ITEM,
                                HttpVerb.DELETE, "delete-item", 200, 409);
                        assertOperation(solution, clientMode, Operation.QUERY,
                                HttpVerb.GET, "query", 200, 404);
                        assertOperation(solution, clientMode, Operation.SCAN,
                                HttpVerb.GET, "scan", 200, 404);
                    }
                },
                10);
    }

    /**
     * Verifies that a conditional PutItem does not reach the required FIFA ID without DynamoDB distance.
     *
     * @throws Throwable when EvoMaster execution fails
     */
    @Test
    public void testConditionalPutItemWithoutDynamoDbHeuristics() throws Throwable {
        String endpoint = "/operations/sync/put-item-heuristic/{fifaId}";

        runTestHandlingFlaky(
                "DynamoDbConditionalPutItemWithoutHeuristicsEM",
                "org.foo.spring.rest.dynamodb.DynamoDbConditionalPutItemWithoutHeuristicsEM",
                500,
                false,
                args -> {
                    configureDynamoDbHeuristics(args, false);
                    setOption(args, "endpointFocus", endpoint);
                    setOption(args, "maxTestSize", "1");
                    Solution<RestIndividual> baseline = initAndRun(args);

                    assertHasAtLeastOne(baseline, HttpVerb.POST, 409, endpoint, "SYNC PUT_ITEM FAILURE");
                    assertNone(baseline, HttpVerb.POST, 201, endpoint, "SYNC PUT_ITEM SUCCESS");
                },
                5);
    }

    /**
     * Verifies that DynamoDB distance guides a conditional PutItem to the required FIFA ID.
     *
     * @throws Throwable when EvoMaster execution fails
     */
    @Test
    public void testConditionalPutItemWithDynamoDbHeuristics() throws Throwable {
        String endpoint = "/operations/sync/put-item-heuristic/{fifaId}";

        runTestHandlingFlaky(
                "DynamoDbConditionalPutItemWithHeuristicsEM",
                "org.foo.spring.rest.dynamodb.DynamoDbConditionalPutItemWithHeuristicsEM",
                1000,
                false,
                args -> {
                    configureDynamoDbHeuristics(args, true);
                    setOption(args, "endpointFocus", endpoint);
                    setOption(args, "maxTestSize", "1");
                    Solution<RestIndividual> guided = initAndRun(args);
                    assertHasAtLeastOne(guided, HttpVerb.POST, 201, endpoint, "SYNC PUT_ITEM SUCCESS");
                },
                5);
    }

    /**
     * Asserts success and failure outcomes for one client-operation pair.
     *
     * @param solution EvoMaster solution
     * @param clientMode SDK client variant
     * @param operation DynamoDB operation
     * @param verb endpoint HTTP verb
     * @param pathSegment operation-specific path segment
     * @param successStatus expected success status
     * @param failureStatus expected failure status
     */
    private void assertOperation(
            Solution<RestIndividual> solution,
            ClientMode clientMode,
            Operation operation,
            HttpVerb verb,
            String pathSegment,
            int successStatus,
            int failureStatus) {
        String mode = clientMode.name().toLowerCase(Locale.ROOT);
        String path = "/operations/" + mode + "/" + pathSegment + "/{existingPlayer}";
        String marker = clientMode.name() + " " + operation.name();
        assertHasAtLeastOne(solution, verb, successStatus, path, marker + " SUCCESS");
        assertHasAtLeastOne(solution, verb, failureStatus, path, marker + " FAILURE");
    }
}
