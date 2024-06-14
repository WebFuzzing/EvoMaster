package org.evomaster.e2etests.spring.rest.mongo.objectid;

import com.foo.spring.rest.mongo.MongoObjectIdAppController;
import org.evomaster.core.EMConfig;
import org.evomaster.core.problem.rest.HttpVerb;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.utils.RestTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class MongoObjectIdWithoutMREMTest extends RestTestBase {

    @BeforeAll
    public static void initClass() throws Exception {
        EMConfig config = new EMConfig();
        config.setInstrumentMR_MONGO(false);
        RestTestBase.initClass(new MongoObjectIdAppController(), config);
    }


    @Test
    public void testRunEMWithoutMongoMethodReplacement() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "MongoObjectIdWithoutMREM",
                "org.foo.spring.rest.mongo.MongoEMGeneration",
                1000,
                (args) -> {
                    args.add("--enableWeightBasedMutationRateSelectionForGene");
                    args.add("false");
                    args.add("--heuristicsForMongo");
                    args.add("false");
                    args.add("--instrumentMR_MONGO");
                    args.add("false");
                    args.add("--generateMongoData");
                    args.add("false");
                    args.add("--extractMongoExecutionInfo");
                    args.add("false");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertFalse(solution.getIndividuals().isEmpty());
                    assertNone(solution, HttpVerb.GET, 200, "/objectid/createObjectId", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/objectid/createObjectId", null);
                });
    }
}