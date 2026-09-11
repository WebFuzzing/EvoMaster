package org.evomaster.e2etests.spring.rest.dynamodb;

import com.foo.spring.rest.dynamodb.WorldCupPlayersEmptyController;
import org.evomaster.core.problem.rest.data.HttpVerb;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/** Verifies that a generated DynamoDB insertion satisfies a failed conditional player read. */
public class DynamoDbInsertionsEMTest extends DynamoDbTestBase {

    /** Starts the empty-table application. */
    @BeforeAll
    public static void initClass() throws Exception {
        initDynamoDbTest(new WorldCupPlayersEmptyController());
    }

    /** Compares the empty-table baseline with insertion-enabled DynamoDB generation. */
    @Test
    public void testInsertionSatisfiesConditionalQuery() throws Throwable {
        runTestHandlingFlakyAndCompilation(
                "DynamoDbInsertionsEM",
                "org.foo.spring.rest.dynamodb.DynamoDbInsertionsEM",
                1000,
                false,
                args -> {
                    List<String> baselineArgs = new ArrayList<>(args);
                    configureDynamoDbInsertions(baselineArgs, false);
                    Solution<RestIndividual> baseline = initAndRun(baselineArgs);
                    assertHasAtLeastOne(baseline, HttpVerb.GET, 404, "/players/{fifaId}", null);
                    assertNone(baseline, HttpVerb.GET, 200, "/players/{fifaId}", null);

                    List<String> insertionArgs = new ArrayList<>(args);
                    configureDynamoDbInsertions(insertionArgs, true);
                    Solution<RestIndividual> generated = initAndRun(insertionArgs);
                    assertHasAtLeastOne(generated, HttpVerb.GET, 200, "/players/{fifaId}",
                            "Lionel Messi plays for Argentina");
                },
                5);
    }
}
