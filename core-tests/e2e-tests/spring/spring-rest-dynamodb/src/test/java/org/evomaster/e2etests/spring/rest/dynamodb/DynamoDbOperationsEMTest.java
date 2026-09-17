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
