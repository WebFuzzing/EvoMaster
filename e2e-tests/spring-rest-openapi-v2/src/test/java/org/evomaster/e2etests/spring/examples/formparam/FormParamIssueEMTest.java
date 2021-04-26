package org.evomaster.e2etests.spring.examples.formparam;

import com.foo.rest.examples.spring.formparam.FormParamController;
import com.foo.rest.examples.spring.formparam.FormParamIssueController;
import org.evomaster.core.problem.rest.HttpVerb;
import org.evomaster.core.problem.rest.RestIndividual;
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