package org.evomaster.e2etests.spring.rest.mongo.findoneby;

import com.foo.spring.rest.mongo.findoneby.MongoFindOneByController;
import org.evomaster.core.EMConfig;
import org.evomaster.core.problem.rest.data.HttpVerb;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.utils.RestTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;

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

        int id = endpoint.length(); //quite brittle

        runTestHandlingFlaky(
                "MongoFindOneByEM_" + id,
                "org.foo.spring.rest.mongo.MongoFindOneByEM"+id,
                1000,
                true,
                (args) -> {
                    setOption(args, "taintForceSelectionOfGenesWithSpecialization", "true");
                    setOption(args,"discoveredInfoRewardedInFitness", "true");

                    setOption(args, "endpointFocus", endpoint);
                    setOption(args, "heuristicsForMongo", "true");
                    setOption(args, "instrumentMR_MONGO", "true");
                    setOption(args, "generateMongoData", "true");
                    setOption(args, "extractMongoExecutionInfo", "true");

                    //issue with generated classes Instantiator and Accessor when running in Maven
                    setOption(args, "minimizeThresholdForLoss", "0.5");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertFalse(solution.getIndividuals().isEmpty());
                    assertHasAtLeastOne(solution, HttpVerb.GET, 400, endpoint, null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, endpoint, null);
                });
    }
}
