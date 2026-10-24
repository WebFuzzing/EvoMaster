package org.evomaster.e2etests.spring.rest.mongo.findoneandupdate;

import com.foo.spring.rest.mongo.findoneandupdate.MongoFindOneAndUpdateAppController;
import org.evomaster.core.EMConfig;
import org.evomaster.core.problem.rest.data.HttpVerb;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.utils.RestTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class MongoFindOneAndUpdateEMTest extends RestTestBase {

    @BeforeAll
    public static void initClass() throws Exception {
        EMConfig config = new EMConfig();
        config.setInstrumentMR_MONGO(true);
        RestTestBase.initClass(new MongoFindOneAndUpdateAppController());
    }

    @Test
    public void testRunEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "MongoFindOneAndUpdateEMTest",
                "org.foo.spring.rest.mongo.MongoFindOneAndUpdateEMTest",
                2000,
                (args) -> {
                    setOption(args, "heuristicsForMongo", "true");
                    setOption(args, "instrumentMR_MONGO", "true");
                    setOption(args, "generateMongoData", "true");
                    setOption(args, "extractMongoExecutionInfo", "true");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    // one endpoint per overload of MongoCollection.findOneAndUpdate.
                    // A 200 needs a document with the requested city and age in the collection,
                    // which can only be generated if the findOneAndUpdate filter is observed

                    // findOneAndUpdate(Bson, Bson)
                    assertHasAtLeastOne(solution, HttpVerb.PUT, 404, "/mongofindoneandupdate/findOneAndUpdateByCityAndAge", null);
                    assertHasAtLeastOne(solution, HttpVerb.PUT, 200, "/mongofindoneandupdate/findOneAndUpdateByCityAndAge", null);

                    // findOneAndUpdate(Bson, Bson, FindOneAndUpdateOptions)
                    assertHasAtLeastOne(solution, HttpVerb.PUT, 404, "/mongofindoneandupdate/findOneAndUpdateByCityAndAgeWithOptions", null);
                    assertHasAtLeastOne(solution, HttpVerb.PUT, 200, "/mongofindoneandupdate/findOneAndUpdateByCityAndAgeWithOptions", null);

                    // findOneAndUpdate(ClientSession, Bson, Bson)
                    assertHasAtLeastOne(solution, HttpVerb.PUT, 404, "/mongofindoneandupdate/findOneAndUpdateByCityAndAgeInSession", null);
                    assertHasAtLeastOne(solution, HttpVerb.PUT, 200, "/mongofindoneandupdate/findOneAndUpdateByCityAndAgeInSession", null);

                    // findOneAndUpdate(ClientSession, Bson, Bson, FindOneAndUpdateOptions)
                    assertHasAtLeastOne(solution, HttpVerb.PUT, 404, "/mongofindoneandupdate/findOneAndUpdateByCityAndAgeInSessionWithOptions", null);
                    assertHasAtLeastOne(solution, HttpVerb.PUT, 200, "/mongofindoneandupdate/findOneAndUpdateByCityAndAgeInSessionWithOptions", null);

                    // findOneAndUpdate(Bson, List<? extends Bson>)
                    assertHasAtLeastOne(solution, HttpVerb.PUT, 404, "/mongofindoneandupdate/findOneAndUpdateByCityAndAgeWithPipeline", null);
                    assertHasAtLeastOne(solution, HttpVerb.PUT, 200, "/mongofindoneandupdate/findOneAndUpdateByCityAndAgeWithPipeline", null);

                    // findOneAndUpdate(Bson, List<? extends Bson>, FindOneAndUpdateOptions)
                    assertHasAtLeastOne(solution, HttpVerb.PUT, 404, "/mongofindoneandupdate/findOneAndUpdateByCityAndAgeWithPipelineAndOptions", null);
                    assertHasAtLeastOne(solution, HttpVerb.PUT, 200, "/mongofindoneandupdate/findOneAndUpdateByCityAndAgeWithPipelineAndOptions", null);

                    // findOneAndUpdate(ClientSession, Bson, List<? extends Bson>)
                    assertHasAtLeastOne(solution, HttpVerb.PUT, 404, "/mongofindoneandupdate/findOneAndUpdateByCityAndAgeInSessionWithPipeline", null);
                    assertHasAtLeastOne(solution, HttpVerb.PUT, 200, "/mongofindoneandupdate/findOneAndUpdateByCityAndAgeInSessionWithPipeline", null);

                    // findOneAndUpdate(ClientSession, Bson, List<? extends Bson>, FindOneAndUpdateOptions)
                    assertHasAtLeastOne(solution, HttpVerb.PUT, 404, "/mongofindoneandupdate/findOneAndUpdateByCityAndAgeInSessionWithPipelineAndOptions", null);
                    assertHasAtLeastOne(solution, HttpVerb.PUT, 200, "/mongofindoneandupdate/findOneAndUpdateByCityAndAgeInSessionWithPipelineAndOptions", null);
                });
    }
}
