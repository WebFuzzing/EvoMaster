package org.evomaster.e2etests.spring.examples.db.tree;

import com.foo.rest.examples.spring.db.tree.DbTreeController;
import com.foo.rest.examples.spring.db.tree.DbTreeRest;
import org.evomaster.core.problem.rest.data.HttpVerb;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class DbTreeEMTest  extends SpringTestBase  {

    @BeforeAll
    public static void initClass() throws Exception {
        SpringTestBase.initClass(new DbTreeController());
    }

    @Test
    public void testRunEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "DbTreeEM",
                "org.bar.db.TreeEM",
                1000,
                (args) -> {

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/db/tree/{id}", DbTreeRest.NOT_FOUND);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/db/tree/{id}", DbTreeRest.NO_PARENT);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/db/tree/{id}", DbTreeRest.WITH_PARENT);
                });
    }
}
