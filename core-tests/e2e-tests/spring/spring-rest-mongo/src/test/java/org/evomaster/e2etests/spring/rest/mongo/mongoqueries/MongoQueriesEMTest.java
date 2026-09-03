package org.evomaster.e2etests.spring.rest.mongo.mongoqueries;

import com.foo.spring.rest.mongo.mongoqueries.MongoQueriesAppController;
import org.evomaster.core.EMConfig;
import org.evomaster.core.problem.rest.data.HttpVerb;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.utils.RestTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class MongoQueriesEMTest extends RestTestBase {

    @BeforeAll
    public static void initClass() throws Exception {
        EMConfig config = new EMConfig();
        config.setInstrumentMR_MONGO(true);
        RestTestBase.initClass(new MongoQueriesAppController(), config);
    }

    @Test
    public void testRunEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "MongoQueriesEMTest",
                "org.foo.spring.rest.mongo.MongoQueriesEMTest",
                3000,
                (args) -> {
                    setOption(args, "heuristicsForMongo", "true");
                    setOption(args, "instrumentMR_MONGO", "true");
                    setOption(args, "generateMongoData", "true");
                    setOption(args, "extractMongoExecutionInfo", "true");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);
                    assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/mongoqueries/saveData", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/mongoqueries/eq", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/mongoqueries/ne", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/mongoqueries/lt", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/mongoqueries/lte", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/mongoqueries/gt", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/mongoqueries/gte", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/mongoqueries/in", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/mongoqueries/nin", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/mongoqueries/mod", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/mongoqueries/not", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/mongoqueries/size", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/mongoqueries/elemMatch", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/mongoqueries/bitsAllClear", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/mongoqueries/bitsAnySet", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/mongoqueries/bitsAllSet", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/mongoqueries/bitsAnyClear", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/mongoqueries/all", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/mongoqueries/type", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/mongoqueries/exists", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/mongoqueries/nor", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/mongoqueries/or", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/mongoqueries/and", null);
                });
    }
}
