package org.evomaster.e2etests.spring.rest.dynamodb;

import com.foo.spring.rest.dynamodb.WorldCupPlayersController;
import org.evomaster.core.problem.rest.data.HttpVerb;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;


/**
 * Verifies that DynamoDB query distance guides EvoMaster to a conditional match.
 */
public class DynamoDbHeuristicsEMTest extends DynamoDbTestBase {

    /**
     * Starts the existing World Cup players SUT.
     *
     * @throws Exception when the embedded controller cannot start
     */
    @BeforeAll
    public static void initClass() throws Exception {
        initDynamoDbTest(new WorldCupPlayersController());
    }

    /**
     * Runs the same search with and without DynamoDB fitness and compares coverage.
     *
     * @throws Throwable when EvoMaster execution fails
     */
    @Test
    public void testConditionalQueryNeedsDynamoDbHeuristics() throws Throwable {
        runTestHandlingFlaky(
                "DynamoDbHeuristicsEM",
                "org.foo.spring.rest.dynamodb.DynamoDbHeuristicsEM",
                1000,
                false,
                args -> {
                    List<String> withoutHeuristics = new ArrayList<>(args);
                    configureDynamoDbHeuristics(withoutHeuristics, false);
                    Solution<RestIndividual> baseline = initAndRun(withoutHeuristics);

                    assertHasAtLeastOne(baseline, HttpVerb.GET, 404, "/players/{fifaId}", null);
                    assertNone(baseline, HttpVerb.GET, 200, "/players/{fifaId}", null);

                    List<String> withHeuristics = new ArrayList<>(args);
                    configureDynamoDbHeuristics(withHeuristics, true);
                    Solution<RestIndividual> guided = initAndRun(withHeuristics);

                    assertHasAtLeastOne(guided, HttpVerb.GET, 200, "/players/{fifaId}",
                            "Lionel Messi plays for Argentina");
                },
                5);
    }
}
