package org.evomaster.e2etests.spring.rest.mongo.delete;

import com.foo.spring.rest.mongo.delete.MongoDeleteAppController;
import org.evomaster.core.EMConfig;
import org.evomaster.core.problem.rest.data.HttpVerb;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.utils.RestTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class MongoDeleteEMTest extends RestTestBase {

    @BeforeAll
    public static void initClass() throws Exception {
        EMConfig config = new EMConfig();
        config.setInstrumentMR_MONGO(true);
        RestTestBase.initClass(new MongoDeleteAppController());
    }

    @Test
    public void testRunEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "MongoDeleteEMTest",
                "org.foo.spring.rest.mongo.MongoDeleteEMTest",
                1000,
                (args) -> {
                    setOption(args, "heuristicsForMongo", "true");
                    setOption(args, "instrumentMR_MONGO", "true");
                    setOption(args, "generateMongoData", "true");
                    setOption(args, "extractMongoExecutionInfo", "true");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    // one endpoint per overload of MongoCollection.deleteOne and MongoCollection.deleteMany

                    // deleteOne(Bson)
                    // a 200 needs a document with the requested city and age in the collection,
                    // which can only be generated if the delete filter is observed
                    assertHasAtLeastOne(solution, HttpVerb.DELETE, 404, "/mongodelete/deleteOneByCityAndAge", null);
                    assertHasAtLeastOne(solution, HttpVerb.DELETE, 200, "/mongodelete/deleteOneByCityAndAge", null);

                    // deleteOne(Bson, DeleteOptions)
                    // a 200 needs a document with the requested city and age in the collection,
                    // which can only be generated if the delete filter is observed
                    assertHasAtLeastOne(solution, HttpVerb.DELETE, 404, "/mongodelete/deleteOneByCityAndAgeWithOptions", null);
                    assertHasAtLeastOne(solution, HttpVerb.DELETE, 200, "/mongodelete/deleteOneByCityAndAgeWithOptions", null);

                    // deleteOne(ClientSession, Bson)
                    // a 200 needs a document with the requested city and age in the collection,
                    // which can only be generated if the delete filter is observed
                    assertHasAtLeastOne(solution, HttpVerb.DELETE, 404, "/mongodelete/deleteOneByCityAndAgeInSession", null);
                    assertHasAtLeastOne(solution, HttpVerb.DELETE, 200, "/mongodelete/deleteOneByCityAndAgeInSession", null);

                    // deleteOne(ClientSession, Bson, DeleteOptions)
                    // a 200 needs a document with the requested city and age in the collection,
                    // which can only be generated if the delete filter is observed
                    assertHasAtLeastOne(solution, HttpVerb.DELETE, 404, "/mongodelete/deleteOneByCityAndAgeInSessionWithOptions", null);
                    assertHasAtLeastOne(solution, HttpVerb.DELETE, 200, "/mongodelete/deleteOneByCityAndAgeInSessionWithOptions", null);

                    // deleteMany(Bson)
                    // a 200 needs a document with the requested city and age in the collection,
                    // which can only be generated if the delete filter is observed
                    assertHasAtLeastOne(solution, HttpVerb.DELETE, 404, "/mongodelete/deleteManyByCityAndAge", null);
                    assertHasAtLeastOne(solution, HttpVerb.DELETE, 200, "/mongodelete/deleteManyByCityAndAge", null);

                    // deleteMany(Bson, DeleteOptions)
                    // a 200 needs a document with the requested city and age in the collection,
                    // which can only be generated if the delete filter is observed
                    assertHasAtLeastOne(solution, HttpVerb.DELETE, 404, "/mongodelete/deleteManyByCityAndAgeWithOptions", null);
                    assertHasAtLeastOne(solution, HttpVerb.DELETE, 200, "/mongodelete/deleteManyByCityAndAgeWithOptions", null);

                    // deleteMany(ClientSession, Bson)
                    // a 200 needs a document with the requested city and age in the collection,
                    // which can only be generated if the delete filter is observed
                    assertHasAtLeastOne(solution, HttpVerb.DELETE, 404, "/mongodelete/deleteManyByCityAndAgeInSession", null);
                    assertHasAtLeastOne(solution, HttpVerb.DELETE, 200, "/mongodelete/deleteManyByCityAndAgeInSession", null);

                    // deleteMany(ClientSession, Bson, DeleteOptions)
                    // a 200 needs a document with the requested city and age in the collection,
                    // which can only be generated if the delete filter is observed
                    assertHasAtLeastOne(solution, HttpVerb.DELETE, 404, "/mongodelete/deleteManyByCityAndAgeInSessionWithOptions", null);
                    assertHasAtLeastOne(solution, HttpVerb.DELETE, 200, "/mongodelete/deleteManyByCityAndAgeInSessionWithOptions", null);
                });
    }
}
