package org.evomaster.e2etests.spring.rest.mongo.findoneanddelete;

import com.foo.spring.rest.mongo.findoneanddelete.MongoFindOneAndDeleteAppController;
import org.evomaster.core.EMConfig;
import org.evomaster.core.problem.rest.data.HttpVerb;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.utils.RestTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class MongoFindOneAndDeleteEMTest extends RestTestBase {

    @BeforeAll
    public static void initClass() throws Exception {
        EMConfig config = new EMConfig();
        config.setInstrumentMR_MONGO(true);
        RestTestBase.initClass(new MongoFindOneAndDeleteAppController());
    }

    @Test
    public void testRunEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "MongoFindOneAndDeleteEMTest",
                "org.foo.spring.rest.mongo.MongoFindOneAndDeleteEMTest",
                1000,
                (args) -> {
                    setOption(args, "heuristicsForMongo", "true");
                    setOption(args, "instrumentMR_MONGO", "true");
                    setOption(args, "generateMongoData", "true");
                    setOption(args, "extractMongoExecutionInfo", "true");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    // one endpoint per overload of MongoCollection.findOneAndDelete.
                    // A 200 needs a document with the requested city and age in the collection,
                    // which can only be generated if the findOneAndDelete filter is observed

                    // findOneAndDelete(Bson)
                    assertHasAtLeastOne(solution, HttpVerb.DELETE, 404, "/mongofindoneanddelete/findOneAndDeleteByCityAndAge", null);
                    assertHasAtLeastOne(solution, HttpVerb.DELETE, 200, "/mongofindoneanddelete/findOneAndDeleteByCityAndAge", null);

                    // findOneAndDelete(Bson, FindOneAndDeleteOptions)
                    assertHasAtLeastOne(solution, HttpVerb.DELETE, 404, "/mongofindoneanddelete/findOneAndDeleteByCityAndAgeWithOptions", null);
                    assertHasAtLeastOne(solution, HttpVerb.DELETE, 200, "/mongofindoneanddelete/findOneAndDeleteByCityAndAgeWithOptions", null);

                    // findOneAndDelete(ClientSession, Bson)
                    assertHasAtLeastOne(solution, HttpVerb.DELETE, 404, "/mongofindoneanddelete/findOneAndDeleteByCityAndAgeInSession", null);
                    assertHasAtLeastOne(solution, HttpVerb.DELETE, 200, "/mongofindoneanddelete/findOneAndDeleteByCityAndAgeInSession", null);

                    // findOneAndDelete(ClientSession, Bson, FindOneAndDeleteOptions)
                    assertHasAtLeastOne(solution, HttpVerb.DELETE, 404, "/mongofindoneanddelete/findOneAndDeleteByCityAndAgeInSessionWithOptions", null);
                    assertHasAtLeastOne(solution, HttpVerb.DELETE, 200, "/mongofindoneanddelete/findOneAndDeleteByCityAndAgeInSessionWithOptions", null);
                });
    }
}
