package org.evomaster.e2etests.spring.rest.opensearch.age;

import com.foo.spring.rest.opensearch.age.OpenSearchAgeController;
import org.evomaster.core.EMConfig;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.utils.RestTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;

public class OpenSearchAgeEMTest extends RestTestBase {

    @BeforeAll
    public static void initClass() throws Exception {
        EMConfig config = new EMConfig();
        config.setInstrumentMR_OPENSEARCH(true);
        RestTestBase.initClass(new OpenSearchAgeController(), config);
    }

    @Disabled("TODO: Enable once we have added support for range queries")
    public void testRunEM() throws Throwable {
        runTestHandlingFlaky(
            "OpenSearchAgeEM",
            "org.foo.spring.rest.opensearch.OpenSearchAgeEM",
            1000,
            true,
            (args) -> {
                setOption(args, "instrumentMR_OPENSEARCH", "true");

                Solution<RestIndividual> solution = initAndRun(args);

//                assertHasAtLeastOne(solution, HttpVerb.GET, 404, "/age/{q}", null);
            });
    }
}
