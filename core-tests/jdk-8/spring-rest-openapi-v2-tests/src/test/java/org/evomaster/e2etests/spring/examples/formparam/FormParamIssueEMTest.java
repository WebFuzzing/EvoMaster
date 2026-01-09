package org.evomaster.e2etests.spring.examples.formparam;

import com.foo.rest.examples.spring.formparam.FormParamIssueController;
import org.evomaster.core.problem.rest.data.HttpVerb;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class FormParamIssueEMTest extends SpringTestBase {

    @BeforeAll
    public static void initClass() throws Exception {

        FormParamIssueController controller = new FormParamIssueController();
        SpringTestBase.initClass(controller);
    }


    @Test
    public void testRunEM() throws Throwable {

        defaultSeed = 0;

        runTestHandlingFlakyAndCompilation(
                "FormParamIssueEM",
                "org.FormParamIssueEM",
                100,
                (args) -> {
                    Solution<RestIndividual> solution = initAndRun(args);

                    assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/formparam", "OK");
                }
        );
    }
}