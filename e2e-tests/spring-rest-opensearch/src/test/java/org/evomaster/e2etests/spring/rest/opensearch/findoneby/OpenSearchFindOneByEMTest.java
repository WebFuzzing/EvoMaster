package org.evomaster.e2etests.spring.rest.opensearch.findoneby;

import com.foo.spring.rest.opensearch.findoneby.OpenSearchFindOneByController;
import org.evomaster.core.EMConfig;
import org.evomaster.core.problem.rest.HttpVerb;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.utils.RestTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class OpenSearchFindOneByEMTest extends RestTestBase {

  @BeforeAll
  public static void initClass() throws Exception {
    EMConfig config = new EMConfig();
    config.setInstrumentMR_MONGO(true);
    RestTestBase.initClass(new OpenSearchFindOneByController(), config);
  }

  @ParameterizedTest
  @ValueSource(strings = {"/findoneby/{id}"})
  public void testFindOneOnGivenEndpoint(String endpoint) throws Throwable {

    int id = endpoint.length(); // quite brittle

    runTestHandlingFlaky(
        "OpenSearchFindOneByEM_" + id,
        "org.foo.spring.rest.opensearch.OpenSearchFindOneByEM" + id,
        1000,
        true,
        (args) -> {
//          setOption(args, "taintForceSelectionOfGenesWithSpecialization", "true");
//          setOption(args, "discoveredInfoRewardedInFitness", "true");
//
//          setOption(args, "endpointFocus", endpoint);
//          setOption(args, "heuristicsForMongo", "true");
//          setOption(args, "instrumentMR_MONGO", "true");
//          setOption(args, "generateMongoData", "true");
//          setOption(args, "extractMongoExecutionInfo", "true");

          // issue with generated classes Instantiator and Accessor when running in Maven
//          setOption(args, "minimizeThresholdForLoss", "0.5");

          Solution<RestIndividual> solution = initAndRun(args);

//          assertFalse(solution.getIndividuals().isEmpty());
//          assertHasAtLeastOne(solution, HttpVerb.GET, 400, endpoint, null);
//          assertHasAtLeastOne(solution, HttpVerb.GET, 200, endpoint, null);
          assertFalse(false);
        });
  }
}
