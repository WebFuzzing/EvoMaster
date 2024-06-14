package org.evomaster.e2etests.spring.rest.mongo.findstring;

import com.foo.spring.rest.mongo.findstring.MongoFindStringController;
import org.evomaster.core.EMConfig;
import org.evomaster.core.problem.rest.HttpVerb;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.utils.RestTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class MongoFindStringEMTest extends RestTestBase {

    @BeforeAll
    public static void initClass() throws Exception {
        EMConfig config = new EMConfig();
        config.setInstrumentMR_MONGO(true);
        RestTestBase.initClass(new MongoFindStringController(), config);
    }

    @Test
    public void testRunEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "MongoFindStringEM",
                "org.foo.spring.rest.mongo.MongoFindStringEM",
                1000,
                (args) -> {
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
                    assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/findstring/{x}", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 404, "/findstring/{x}", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/findstring/{x}", null);
                });
    }
}