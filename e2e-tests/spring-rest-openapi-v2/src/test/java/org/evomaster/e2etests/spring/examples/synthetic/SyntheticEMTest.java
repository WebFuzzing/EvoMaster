package org.evomaster.e2etests.spring.examples.synthetic;

import com.foo.rest.examples.spring.stringminlength.StringMinLengthController;
import com.foo.rest.examples.spring.synthetic.SyntheticController;
import org.evomaster.core.problem.rest.HttpVerb;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class SyntheticEMTest extends SpringTestBase {

    @BeforeAll
    public static void initClass() throws Exception {
        SpringTestBase.initClass(new SyntheticController());
    }

    @Test
    public void testRunEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "SyntheticEM",
                100,
                (args) -> {
                    Solution<RestIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/synthetic/{s}", "NOPE");
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/synthetic/{s}", "OK");
                });
    }
}
