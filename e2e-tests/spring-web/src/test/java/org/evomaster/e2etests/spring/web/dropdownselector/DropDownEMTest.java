package org.evomaster.e2etests.spring.web.dropdownselector;

import com.foo.web.examples.spring.dropdownselector.DropDownController;
import org.evomaster.core.problem.webfrontend.WebIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.web.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class DropDownEMTest extends SpringTestBase {
    @BeforeAll
    public static void initClass() throws Exception {
        SpringTestBase.initClass(new DropDownController());
    }

    @Disabled
    @Test
    public void testRunEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "DropDownEM",
                "org.DropDownEM",
                50,
                (args) -> {

                    Solution<WebIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() > 0);

                    assertHasVisitedUrlPath(solution, "/dropdown/index.html", "/dropdown/page1.html", "/dropdown/page2.html", "/dropdown/page3.html");
                    assertNoHtmlErrors(solution); // statement ok - gives no errors
                }
        );
    }

}
