package org.evomaster.e2etests.spring.examples.postcollection;

import com.foo.rest.examples.spring.postcollection.PostCollectionController;
import org.evomaster.core.Main;
import org.evomaster.core.problem.rest.HttpVerb;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class PCEMTest extends SpringTestBase {

    @BeforeAll
    public static void initClass() throws Exception {

        SpringTestBase.initClass(new PostCollectionController());
    }

    @Test
    public void testRunEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "PcEM",
                "org.bar.PcEM",
                1_000,
                (args) -> {

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    assertHasAtLeastOne(solution, HttpVerb.POST, 201);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 400);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200);
                });
    }
}
