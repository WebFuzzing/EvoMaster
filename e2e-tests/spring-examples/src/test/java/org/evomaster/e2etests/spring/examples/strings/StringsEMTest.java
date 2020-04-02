package org.evomaster.e2etests.spring.examples.strings;

import org.evomaster.core.Main;
import org.evomaster.core.problem.rest.HttpVerb;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.Solution;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class StringsEMTest extends StringsTestBase {

    @Test
    public void testDeterminism(){

        runAndCheckDeterminism(1_000, (args) -> {
            initAndRun(args);
        });
    }

    @Test
    public void testRunEM() throws Throwable {

        defaultSeed = 13;

        runTestHandlingFlakyAndCompilation(
                "StringsEM",
                "org.bar.StringsEM",
                10_000,
                (args) -> {

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/strings/equalsFoo/{s}", "false");
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/strings/contains/{s}", "false");
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/strings/startEnds/{s}", "false");


                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/strings/equalsFoo/{s}", "true");
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/strings/contains/{s}", "true");
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/strings/startEnds/{s}", "true");
                    assertHasAtLeastOne(solution, HttpVerb.POST, 500);
                });
    }
}
