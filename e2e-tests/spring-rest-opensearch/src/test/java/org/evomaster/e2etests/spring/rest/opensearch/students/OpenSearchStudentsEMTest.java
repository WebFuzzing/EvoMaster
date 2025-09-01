package org.evomaster.e2etests.spring.rest.opensearch.students;

import com.foo.spring.rest.opensearch.students.OpenSearchStudentsController;
import org.evomaster.core.EMConfig;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.utils.RestTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;

public class OpenSearchStudentsEMTest extends RestTestBase {

    @BeforeAll
    public static void initClass() throws Exception {
        EMConfig config = new EMConfig();
        config.setInstrumentMR_OPENSEARCH(true);
        RestTestBase.initClass(new OpenSearchStudentsController(), config);
    }

    @Disabled("TODO: Enable once we have added support for query generation")
    public void testRunEM() throws Throwable {
        runTestHandlingFlaky(
            "OpenSearchStudentsEM",
            "org.foo.spring.rest.opensearch.OpenSearchStudentsEM",
            1000,
            true,
            (args) -> {
                setOption(args, "instrumentMR_OPENSEARCH", "true");

                Solution<RestIndividual> solution = initAndRun(args);

//                assertHasAtLeastOne(solution, HttpVerb.GET, 404, "/students/{q}", null);
            });
    }
}
