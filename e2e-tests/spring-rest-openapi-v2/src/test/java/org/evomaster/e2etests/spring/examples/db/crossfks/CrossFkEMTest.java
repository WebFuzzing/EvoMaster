package org.evomaster.e2etests.spring.examples.db.crossfks;

import com.foo.rest.examples.spring.db.base.DbBaseController;
import com.foo.rest.examples.spring.db.crossfks.CrossFkController;
import org.evomaster.core.problem.rest.HttpVerb;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.evomaster.e2etests.spring.examples.db.base.DbBaseTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class CrossFkEMTest extends SpringTestBase {

    @BeforeAll
    public static void initClass() throws Exception {

        SpringTestBase.initClass(new CrossFkController());
    }
    @Test
    public void testRunEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "CrossFkEM",
                "org.bar.db.CrossFkEM",
                10_000,
                (args) -> {

                    args.add("--probOfEnablingSingleInsertionForTable");
                    args.add("1.0");
                    args.add("--taintOnSampling");
                    args.add("true");


                    Solution<RestIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/root/{rootName}/foo/{fooName}/bar", "NOT EMPTY");
                });
    }
}
