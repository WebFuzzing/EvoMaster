package org.evomaster.e2etests.spring.web.alinks;

import com.foo.web.examples.spring.alinks.ALinksController;
import org.evomaster.core.problem.webfrontend.WebIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.web.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ALinksEMTest extends SpringTestBase {

    @BeforeAll
    public static void initClass() throws Exception {
        SpringTestBase.initClass(new ALinksController());
    }

    @Test
    public void testRunEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "ALinksEM",
                "org.ALinksEM",
                50,
                (args) -> {

                    Solution<WebIndividual> solution = initAndRun(args);

                    //TODO assertions
                }
        );
    }
}
