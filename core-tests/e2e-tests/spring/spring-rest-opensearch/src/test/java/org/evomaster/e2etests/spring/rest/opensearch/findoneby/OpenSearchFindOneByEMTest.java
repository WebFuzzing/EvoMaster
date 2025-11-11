package org.evomaster.e2etests.spring.rest.opensearch.findoneby;

import com.foo.spring.rest.opensearch.findoneby.OpenSearchFindOneByController;
import org.evomaster.core.EMConfig;
import org.evomaster.core.problem.rest.data.HttpVerb;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.utils.RestTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class OpenSearchFindOneByEMTest extends RestTestBase {

  @BeforeAll
  public static void initClass() throws Exception {
    EMConfig config = new EMConfig();
    config.setInstrumentMR_OPENSEARCH(true);
    RestTestBase.initClass(new OpenSearchFindOneByController(), config);
  }

  @ParameterizedTest
  @ValueSource(strings = {"/findoneby/{id}"})
  public void testFindOneOnGivenEndpoint(String endpoint) throws Throwable {

    int id = endpoint.length();

    runTestHandlingFlaky(
        "OpenSearchFindOneByEM_" + id,
        "org.foo.spring.rest.opensearch.OpenSearchFindOneByEM" + id,
        1000,
        true,
        (args) -> {
          setOption(args, "instrumentMR_OPENSEARCH", "true");

          Solution<RestIndividual> solution = initAndRun(args);

          assertHasAtLeastOne(solution, HttpVerb.GET, 404, endpoint, null);
        });
  }
}
