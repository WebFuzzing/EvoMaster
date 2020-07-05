package org.evomaster.e2etests.spring.examples.ttpaper;

import com.foo.rest.examples.spring.ttpaper.TTPaperController;
import org.evomaster.core.problem.rest.HttpVerb;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.evomaster.e2etests.utils.CIUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TTPaperEMTest  extends SpringTestBase {


    @BeforeAll
    public static void initClass() throws Exception {
        SpringTestBase.initClass(new TTPaperController());
    }


    @Test
    public void testNumeric() throws Throwable {

        defaultSeed = 0;

        CIUtils.skipIfOnCircleCI();

        runTestHandlingFlakyAndCompilation(
                "TTPaperNumeric",
                "org.bar.ttpaper.TTPaperNumeric",
                10_000,
                true,
                (args) -> {

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/numeric/{x}", "OK");
                },
                3);
    }

}
