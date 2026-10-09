package org.evomaster.e2etests.spring.rest.mongo.update;

import com.foo.spring.rest.mongo.update.MongoUpdateAppController;
import org.evomaster.core.EMConfig;
import org.evomaster.core.problem.rest.data.HttpVerb;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.utils.RestTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class MongoUpdateEMTest extends RestTestBase {

    @BeforeAll
    public static void initClass() throws Exception {
        EMConfig config = new EMConfig();
        config.setInstrumentMR_MONGO(true);
        RestTestBase.initClass(new MongoUpdateAppController());
    }

    @Test
    public void testRunEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "MongoUpdateEMTest",
                "org.foo.spring.rest.mongo.MongoUpdateEMTest",
                2000,
                (args) -> {
                    setOption(args, "heuristicsForMongo", "true");
                    setOption(args, "instrumentMR_MONGO", "true");
                    setOption(args, "generateMongoData", "true");
                    setOption(args, "extractMongoExecutionInfo", "true");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    // one endpoint per overload of MongoCollection.updateOne and MongoCollection.updateMany.
                    // A 200 needs a document with the requested city and age in the collection,
                    // which can only be generated if the update filter is observed

                    // updateOne(Bson, Bson)
                    assertHasAtLeastOne(solution, HttpVerb.PUT, 404, "/mongoupdate/updateOneByCityAndAge", null);
                    assertHasAtLeastOne(solution, HttpVerb.PUT, 200, "/mongoupdate/updateOneByCityAndAge", null);

                    // updateOne(Bson, Bson, UpdateOptions)
                    assertHasAtLeastOne(solution, HttpVerb.PUT, 404, "/mongoupdate/updateOneByCityAndAgeWithOptions", null);
                    assertHasAtLeastOne(solution, HttpVerb.PUT, 200, "/mongoupdate/updateOneByCityAndAgeWithOptions", null);

                    // updateOne(ClientSession, Bson, Bson)
                    assertHasAtLeastOne(solution, HttpVerb.PUT, 404, "/mongoupdate/updateOneByCityAndAgeInSession", null);
                    assertHasAtLeastOne(solution, HttpVerb.PUT, 200, "/mongoupdate/updateOneByCityAndAgeInSession", null);

                    // updateOne(ClientSession, Bson, Bson, UpdateOptions)
                    assertHasAtLeastOne(solution, HttpVerb.PUT, 404, "/mongoupdate/updateOneByCityAndAgeInSessionWithOptions", null);
                    assertHasAtLeastOne(solution, HttpVerb.PUT, 200, "/mongoupdate/updateOneByCityAndAgeInSessionWithOptions", null);

                    // updateOne(Bson, List<? extends Bson>)
                    assertHasAtLeastOne(solution, HttpVerb.PUT, 404, "/mongoupdate/updateOneByCityAndAgeWithPipeline", null);
                    assertHasAtLeastOne(solution, HttpVerb.PUT, 200, "/mongoupdate/updateOneByCityAndAgeWithPipeline", null);

                    // updateOne(Bson, List<? extends Bson>, UpdateOptions)
                    assertHasAtLeastOne(solution, HttpVerb.PUT, 404, "/mongoupdate/updateOneByCityAndAgeWithPipelineAndOptions", null);
                    assertHasAtLeastOne(solution, HttpVerb.PUT, 200, "/mongoupdate/updateOneByCityAndAgeWithPipelineAndOptions", null);

                    // updateOne(ClientSession, Bson, List<? extends Bson>)
                    assertHasAtLeastOne(solution, HttpVerb.PUT, 404, "/mongoupdate/updateOneByCityAndAgeInSessionWithPipeline", null);
                    assertHasAtLeastOne(solution, HttpVerb.PUT, 200, "/mongoupdate/updateOneByCityAndAgeInSessionWithPipeline", null);

                    // updateOne(ClientSession, Bson, List<? extends Bson>, UpdateOptions)
                    assertHasAtLeastOne(solution, HttpVerb.PUT, 404, "/mongoupdate/updateOneByCityAndAgeInSessionWithPipelineAndOptions", null);
                    assertHasAtLeastOne(solution, HttpVerb.PUT, 200, "/mongoupdate/updateOneByCityAndAgeInSessionWithPipelineAndOptions", null);

                    // updateMany(Bson, Bson)
                    assertHasAtLeastOne(solution, HttpVerb.PUT, 404, "/mongoupdate/updateManyByCityAndAge", null);
                    assertHasAtLeastOne(solution, HttpVerb.PUT, 200, "/mongoupdate/updateManyByCityAndAge", null);

                    // updateMany(Bson, Bson, UpdateOptions)
                    assertHasAtLeastOne(solution, HttpVerb.PUT, 404, "/mongoupdate/updateManyByCityAndAgeWithOptions", null);
                    assertHasAtLeastOne(solution, HttpVerb.PUT, 200, "/mongoupdate/updateManyByCityAndAgeWithOptions", null);

                    // updateMany(ClientSession, Bson, Bson)
                    assertHasAtLeastOne(solution, HttpVerb.PUT, 404, "/mongoupdate/updateManyByCityAndAgeInSession", null);
                    assertHasAtLeastOne(solution, HttpVerb.PUT, 200, "/mongoupdate/updateManyByCityAndAgeInSession", null);

                    // updateMany(ClientSession, Bson, Bson, UpdateOptions)
                    assertHasAtLeastOne(solution, HttpVerb.PUT, 404, "/mongoupdate/updateManyByCityAndAgeInSessionWithOptions", null);
                    assertHasAtLeastOne(solution, HttpVerb.PUT, 200, "/mongoupdate/updateManyByCityAndAgeInSessionWithOptions", null);

                    // updateMany(Bson, List<? extends Bson>)
                    assertHasAtLeastOne(solution, HttpVerb.PUT, 404, "/mongoupdate/updateManyByCityAndAgeWithPipeline", null);
                    assertHasAtLeastOne(solution, HttpVerb.PUT, 200, "/mongoupdate/updateManyByCityAndAgeWithPipeline", null);

                    // updateMany(Bson, List<? extends Bson>, UpdateOptions)
                    assertHasAtLeastOne(solution, HttpVerb.PUT, 404, "/mongoupdate/updateManyByCityAndAgeWithPipelineAndOptions", null);
                    assertHasAtLeastOne(solution, HttpVerb.PUT, 200, "/mongoupdate/updateManyByCityAndAgeWithPipelineAndOptions", null);

                    // updateMany(ClientSession, Bson, List<? extends Bson>)
                    assertHasAtLeastOne(solution, HttpVerb.PUT, 404, "/mongoupdate/updateManyByCityAndAgeInSessionWithPipeline", null);
                    assertHasAtLeastOne(solution, HttpVerb.PUT, 200, "/mongoupdate/updateManyByCityAndAgeInSessionWithPipeline", null);

                    // updateMany(ClientSession, Bson, List<? extends Bson>, UpdateOptions)
                    assertHasAtLeastOne(solution, HttpVerb.PUT, 404, "/mongoupdate/updateManyByCityAndAgeInSessionWithPipelineAndOptions", null);
                    assertHasAtLeastOne(solution, HttpVerb.PUT, 200, "/mongoupdate/updateManyByCityAndAgeInSessionWithPipelineAndOptions", null);
                });
    }
}
