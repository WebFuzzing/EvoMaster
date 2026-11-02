package org.evomaster.e2etests.spring.rest.mongo.findoneandreplace;

import com.foo.spring.rest.mongo.findoneandreplace.MongoFindOneAndReplaceAppController;
import org.evomaster.core.EMConfig;
import org.evomaster.core.problem.rest.data.HttpVerb;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.utils.RestTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class MongoFindOneAndReplaceEMTest extends RestTestBase {

    @BeforeAll
    public static void initClass() throws Exception {
        EMConfig config = new EMConfig();
        config.setInstrumentMR_MONGO(true);
        RestTestBase.initClass(new MongoFindOneAndReplaceAppController());
    }

    @Test
    public void testRunEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "MongoFindOneAndReplaceEMTest",
                "org.foo.spring.rest.mongo.MongoFindOneAndReplaceEMTest",
                1000,
                (args) -> {
                    setOption(args, "heuristicsForMongo", "true");
                    setOption(args, "instrumentMR_MONGO", "true");
                    setOption(args, "generateMongoData", "true");
                    setOption(args, "extractMongoExecutionInfo", "true");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    // one endpoint per overload of MongoCollection.findOneAndReplace.
                    // A 200 needs a document with the requested city and age in the collection,
                    // which can only be generated if the findOneAndReplace filter is observed

                    // findOneAndReplace(Bson, TDocument)
                    assertHasAtLeastOne(solution, HttpVerb.PUT, 404, "/mongofindoneandreplace/findOneAndReplaceByCityAndAge", null);
                    assertHasAtLeastOne(solution, HttpVerb.PUT, 200, "/mongofindoneandreplace/findOneAndReplaceByCityAndAge", null);

                    // findOneAndReplace(Bson, TDocument, FindOneAndReplaceOptions)
                    assertHasAtLeastOne(solution, HttpVerb.PUT, 404, "/mongofindoneandreplace/findOneAndReplaceByCityAndAgeWithOptions", null);
                    assertHasAtLeastOne(solution, HttpVerb.PUT, 200, "/mongofindoneandreplace/findOneAndReplaceByCityAndAgeWithOptions", null);

                    // findOneAndReplace(ClientSession, Bson, TDocument)
                    assertHasAtLeastOne(solution, HttpVerb.PUT, 404, "/mongofindoneandreplace/findOneAndReplaceByCityAndAgeInSession", null);
                    assertHasAtLeastOne(solution, HttpVerb.PUT, 200, "/mongofindoneandreplace/findOneAndReplaceByCityAndAgeInSession", null);

                    // findOneAndReplace(ClientSession, Bson, TDocument, FindOneAndReplaceOptions)
                    assertHasAtLeastOne(solution, HttpVerb.PUT, 404, "/mongofindoneandreplace/findOneAndReplaceByCityAndAgeInSessionWithOptions", null);
                    assertHasAtLeastOne(solution, HttpVerb.PUT, 200, "/mongofindoneandreplace/findOneAndReplaceByCityAndAgeInSessionWithOptions", null);
                });
    }
}
