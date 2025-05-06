package org.evomaster.e2etests.spring.examples.ttpaper;

import com.foo.rest.examples.spring.ttpaper.TTPaperController;
import org.evomaster.core.problem.rest.data.HttpVerb;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
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

        runTestHandlingFlakyAndCompilation(
                "TTPaper_numeric",
                "org.bar.ttpaper.TTPaperNumeric",
                10_000,
                true,
                (args) -> {
                    args.add("--endpointFocus=/api/numeric/{x}");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/numeric/{x}", "OK");
                },
                3);
    }

    @Test
    public void testParam() throws Throwable {

        defaultSeed = 0;

        runTestHandlingFlakyAndCompilation(
                "TTPaper_param",
                "org.bar.ttpaper.TTPaperParam",
                1_000,
                true,
                (args) -> {

                    args.add("--endpointFocus=/api/param");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/param", "OK");
                },
                3);
    }


    @Test
    public void testBody() throws Throwable {

        defaultSeed = 0;

        runTestHandlingFlakyAndCompilation(
                "TTPaper_body",
                "org.bar.ttpaper.TTPaperBody",
                1_000,
                true,
                (args) -> {

                    args.add("--endpointFocus=/api/body");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/body", "OK");
                },
                3);
    }
}
