package org.evomaster.e2etests.spring.examples.bodyissue;

import com.foo.rest.examples.spring.bodyissue.BodyIssueController;
import org.evomaster.core.problem.rest.data.HttpVerb;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Created by arcuri82 on 07-Nov-18.
 */
public class BodyIssueEMTest extends SpringTestBase {

    @BeforeAll
    public static void initClass() throws Exception {

        SpringTestBase.initClass(new BodyIssueController());
    }


    @Test
    public void testRunEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "BodyIssueEM",
                "org.BodyIssueEM",
                50,
                (args) -> {

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/bodyissue", "42");
                    assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/bodyissue", "123");
                }
        );
    }
}