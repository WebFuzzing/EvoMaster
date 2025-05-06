package org.evomaster.e2etests.spring.examples.insertcollection;

import com.foo.rest.examples.spring.db.insertcollection.RResourceController;
import org.evomaster.core.problem.rest.data.HttpVerb;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * created by manzhang on 2021/11/10
 */
public class SqlCEMTest extends SpringTestBase {

    @BeforeAll
    public static void initClass() throws Exception {

        SpringTestBase.initClass(new RResourceController());
    }

    @Test
    public void testRunEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "SqlCEM",
                "org.bar.SqlCEM",
                1_000,
                (args) -> {

                    args.add("--initStructureMutationProbability");
                    args.add("0.5");

                    args.add("--maxSizeOfMutatingInitAction");
                    args.add("5");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    assertHasAtLeastOne(solution, HttpVerb.GET, 400);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200);
                });
    }

}
