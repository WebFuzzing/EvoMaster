package org.evomaster.e2etests.spring.examples.regexdate;

import com.foo.rest.examples.spring.regex.RegexController;
import com.foo.rest.examples.spring.regexdate.RegexDateController;
import org.evomaster.core.problem.rest.HttpVerb;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by arcuri82 on 12-Jun-19.
 */
public class RegexDateEMTest extends SpringTestBase {

    @BeforeAll
    public static void initClass() throws Exception {

        SpringTestBase.initClass(new RegexDateController());
    }

    @Test
    public void testRunEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "RegexDateEM",
                "org.bar.RegexDateEM",
                1000,
                (args) -> {

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    assertHasAtLeastOne(solution, HttpVerb.GET, 500);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200);
                });
    }
}