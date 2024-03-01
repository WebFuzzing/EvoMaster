package org.evomaster.e2etests.spring.rest.mongo.personswithoutpost;

import com.foo.spring.rest.mongo.MongoPersonsWithoutPostAppController;
import org.evomaster.core.EMConfig;
import org.evomaster.core.problem.rest.HttpVerb;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.utils.RestTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class MongoPersonsWithoutPostEMTest extends RestTestBase {

    @BeforeAll
    public static void initClass() throws Exception {
        EMConfig config = new EMConfig();
        config.setInstrumentMR_MONGO(true);
        RestTestBase.initClass(new MongoPersonsWithoutPostAppController(), config);
    }

    @Test
    public void testRunEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "MongoPersonsWithoutPostEM",
                "org.foo.spring.rest.mongo.MongoEMGeneration",
                1000,
                (args) -> {
                    args.add("--enableWeightBasedMutationRateSelectionForGene");
                    args.add("false");
                    args.add("--heuristicsForMongo");
                    args.add("true");
                    args.add("--instrumentMR_MONGO");
                    args.add("true");
                    args.add("--generateMongoData");
                    args.add("true");
                    args.add("--extractMongoExecutionInfo");
                    args.add("true");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/persons/list18", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/persons/list18", null);
                });
    }
}