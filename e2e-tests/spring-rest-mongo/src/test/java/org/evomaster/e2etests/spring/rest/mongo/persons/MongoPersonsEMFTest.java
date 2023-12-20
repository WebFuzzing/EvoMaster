package org.evomaster.e2etests.spring.rest.mongo.persons;

import com.foo.spring.rest.mongo.MongoPersonsAppController;
import org.evomaster.core.problem.rest.HttpVerb;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.utils.RestTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class MongoPersonsEMFTest extends RestTestBase {

    @BeforeAll
    public static void initClass() throws Exception {
        RestTestBase.initClass(new MongoPersonsAppController());
    }

    @Test
    public void testRunEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "MongoPersonsEM",
                "org.foo.spring.rest.mongo.MongoEMFitness",
                10000,
                (args) -> {
                    args.add("--enableWeightBasedMutationRateSelectionForGene");
                    args.add("false");
                    args.add("--heuristicsForMongo");
                    args.add("true");
                    args.add("--instrumentMR_MONGO");
                    args.add("true");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);
                    assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/persons/{age}", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/persons/list18", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/persons/list18", null);
                });
    }
}