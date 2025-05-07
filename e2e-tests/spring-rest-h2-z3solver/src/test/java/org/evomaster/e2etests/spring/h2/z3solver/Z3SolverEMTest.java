package org.evomaster.e2etests.spring.h2.z3solver;

import com.foo.spring.rest.h2.z3solver.Z3SolverController;
import org.evomaster.core.problem.rest.data.HttpVerb;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class Z3SolverEMTest extends SpringTestBase {

    @BeforeAll
    public static void initClass() throws Exception {

        SpringTestBase.initClass(new Z3SolverController());
    }

    @Test
    public void testRunEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "Z3SolverEM",
                "com.foo.spring.rest.h2.z3solver.Z3SolverEvoMaster",
                50,
                (args) -> {
                    args.add("--heuristicsForSQL");
                    args.add("true");
                    args.add("--generateSqlDataWithSearch");
                    args.add("false");
                    args.add("--generateSqlDataWithDSE");
                    args.add("true");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertFalse(solution.getIndividuals().isEmpty());

                    // TODO: Add support for queries with empty WHERE in the select
                    // assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/h2/z3solver/products", null);
                    // assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/h2/z3solver/products", null);

                    assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/h2/z3solver/products-1", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/h2/z3solver/products-1", null);

                    // TODO: This is currently not supported
                    // assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/h2/z3solver/products-2/{id}", null);
                    // assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/h2/z3solver/products-2/{id}", null);

                    assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/h2/z3solver/products-3", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/h2/z3solver/products-3", null);
                });
    }
}
