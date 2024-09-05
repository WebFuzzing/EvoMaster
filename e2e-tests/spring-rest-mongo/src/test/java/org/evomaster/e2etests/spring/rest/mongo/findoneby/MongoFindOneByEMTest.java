package org.evomaster.e2etests.spring.rest.mongo.findoneby;

import com.foo.spring.rest.mongo.findoneby.MongoFindOneByController;
import org.evomaster.core.EMConfig;
import org.evomaster.core.problem.rest.HttpVerb;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.utils.RestTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class MongoFindOneByEMTest extends RestTestBase {

    @BeforeAll
    public static void initClass() throws Exception {
        EMConfig config = new EMConfig();
        config.setInstrumentMR_MONGO(true);
        RestTestBase.initClass(new MongoFindOneByController(), config);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/findoneby/id/{id}",
            "/findoneby/type/{type}",
            "/findoneby/source/{source}",
            "/findoneby/idtype/{id}/{type}",
            "/findoneby/sourcetypeid/{source}/{type}/{id}"})
    public void testFindOneOnGivenEndpoint(String endpoint) throws Throwable {

        runTestHandlingFlaky(
                "MongoFindOneByEM",
                "org.foo.spring.rest.mongo.MongoFindOneByEM",
                500,
                true,
                (args) -> {
                    setOption(args, "endpointFocus", endpoint);
                    setOption(args, "heuristicsForMongo", "true");
                    setOption(args, "instrumentMR_MONGO", "true");
                    setOption(args, "generateMongoData", "true");
                    setOption(args, "extractMongoExecutionInfo", "true");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 400, endpoint, null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, endpoint, null);
                });
    }
}
