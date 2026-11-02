package org.evomaster.e2etests.spring.rest.mongo.count;

import com.foo.spring.rest.mongo.count.MongoCountAppController;
import org.evomaster.core.EMConfig;
import org.evomaster.core.problem.rest.data.HttpVerb;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.utils.RestTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class MongoCountEMTest extends RestTestBase {

    @BeforeAll
    public static void initClass() throws Exception {
        EMConfig config = new EMConfig();
        config.setInstrumentMR_MONGO(true);
        RestTestBase.initClass(new MongoCountAppController());
    }

    @Test
    public void testRunEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "MongoCountEMTest",
                "org.foo.spring.rest.mongo.MongoCountEMTest",
                500,
                (args) -> {
                    setOption(args, "heuristicsForMongo", "true");
                    setOption(args, "instrumentMR_MONGO", "true");
                    setOption(args, "generateMongoData", "true");
                    setOption(args, "extractMongoExecutionInfo", "true");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    // one endpoint per overload of MongoCollection.countDocuments

                    // countDocuments()
                    // no filter to learn from: only the empty-collection case is guaranteed
                    assertHasAtLeastOne(solution, HttpVerb.GET, 404, "/mongocount/countAll", null);

                    // countDocuments(Bson)
                    // a 200 needs a document with the requested city and age in the collection,
                    // which can only be generated if the countDocuments filter is observed
                    assertHasAtLeastOne(solution, HttpVerb.GET, 404, "/mongocount/countByCityAndAge", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/mongocount/countByCityAndAge", null);

                    // countDocuments(Bson, CountOptions)
                    assertHasAtLeastOne(solution, HttpVerb.GET, 404, "/mongocount/countByCityAndAgeWithOptions", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/mongocount/countByCityAndAgeWithOptions", null);

                    // countDocuments(ClientSession)
                    // no filter to learn from: only the empty-collection case is guaranteed
                    assertHasAtLeastOne(solution, HttpVerb.GET, 404, "/mongocount/countAllInSession", null);

                    // countDocuments(ClientSession, Bson)
                    assertHasAtLeastOne(solution, HttpVerb.GET, 404, "/mongocount/countByCityAndAgeInSession", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/mongocount/countByCityAndAgeInSession", null);

                    // countDocuments(ClientSession, Bson, CountOptions)
                    assertHasAtLeastOne(solution, HttpVerb.GET, 404, "/mongocount/countByCityAndAgeInSessionWithOptions", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/mongocount/countByCityAndAgeInSessionWithOptions", null);
                });
    }
}
