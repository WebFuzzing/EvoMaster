package org.evomaster.e2etests.spring.rest.dynamodb;

import com.foo.spring.rest.dynamodb.WorldCupPlayersController;
import org.evomaster.core.EMConfig;
import org.evomaster.core.problem.rest.data.HttpVerb;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.utils.RestTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;


/**
 * Verifies that DynamoDB query distance guides EvoMaster to a conditional match.
 */
public class DynamoDbHeuristicsEMTest extends RestTestBase {

    private static final String INSTRUMENT_DYNAMODB_OPTION = "instrumentMR_DYNAMODB";
    private static final String DYNAMODB_HEURISTICS_OPTION = "heuristicsForDynamoDb";

    /**
     * Starts the instrumented DynamoDB SUT once for this test class.
     *
     * @throws Exception when the embedded controller cannot start
     */
    @BeforeAll
    public static void initClass() throws Exception {
        EMConfig config = new EMConfig();
        config.setInstrumentMR_DYNAMODB(true);
        RestTestBase.initClass(new WorldCupPlayersController(), config);
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
                    setOption(withoutHeuristics, INSTRUMENT_DYNAMODB_OPTION, "true");
                    setOption(withoutHeuristics, DYNAMODB_HEURISTICS_OPTION, "false");
                    Solution<RestIndividual> baseline = initAndRun(withoutHeuristics);

                    assertHasAtLeastOne(baseline, HttpVerb.GET, 404, "/players/{fifaId}", null);
                    assertNone(baseline, HttpVerb.GET, 200, "/players/{fifaId}", null);

                    List<String> withHeuristics = new ArrayList<>(args);
                    setOption(withHeuristics, INSTRUMENT_DYNAMODB_OPTION, "true");
                    setOption(withHeuristics, DYNAMODB_HEURISTICS_OPTION, "true");
                    Solution<RestIndividual> guided = initAndRun(withHeuristics);

                    assertHasAtLeastOne(guided, HttpVerb.GET, 200, "/players/{fifaId}",
                            "Lionel Messi plays for Argentina");
                },
                5);
    }
}
