package org.evomaster.e2etests.spring.rest.mongo.repository;

import com.foo.spring.rest.mongo.repository.MongoRepositoryAppController;
import org.evomaster.core.EMConfig;
import org.evomaster.core.problem.rest.data.HttpVerb;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.utils.RestTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class MongoRepositoryEMTest extends RestTestBase {

    @BeforeAll
    public static void initClass() throws Exception {
        EMConfig config = new EMConfig();
        config.setInstrumentMR_MONGO(true);
        RestTestBase.initClass(new MongoRepositoryAppController());
    }

    @Test
    public void testRunEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "MongoRepositoryEMTest",
                "org.foo.spring.rest.mongo.MongoRepositoryEMTest",
                1500,
                (args) -> {
                    setOption(args, "heuristicsForMongo", "true");
                    setOption(args, "instrumentMR_MONGO", "true");
                    setOption(args, "generateMongoData", "true");
                    setOption(args, "extractMongoExecutionInfo", "true");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    // exists/count/delete through Spring Data repositories.
                    // A 200 needs a document with the requested city and age in the collection,
                    // which can only be generated if the query run by Spring Data is observed

                    // derived exists query
                    assertHasAtLeastOne(solution, HttpVerb.GET, 404, "/mongorepository/existsDerived", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/mongorepository/existsDerived", null);

                    // annotated exists query
                    assertHasAtLeastOne(solution, HttpVerb.GET, 404, "/mongorepository/existsAnnotated", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/mongorepository/existsAnnotated", null);

                    // derived count query
                    assertHasAtLeastOne(solution, HttpVerb.GET, 404, "/mongorepository/countDerived", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/mongorepository/countDerived", null);

                    // annotated count query
                    assertHasAtLeastOne(solution, HttpVerb.GET, 404, "/mongorepository/countAnnotated", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/mongorepository/countAnnotated", null);

                    // derived delete query
                    assertHasAtLeastOne(solution, HttpVerb.DELETE, 404, "/mongorepository/deleteDerived", null);
                    assertHasAtLeastOne(solution, HttpVerb.DELETE, 200, "/mongorepository/deleteDerived", null);

                    // annotated delete query
                    assertHasAtLeastOne(solution, HttpVerb.DELETE, 404, "/mongorepository/deleteAnnotated", null);
                    assertHasAtLeastOne(solution, HttpVerb.DELETE, 200, "/mongorepository/deleteAnnotated", null);
                });
    }
}
