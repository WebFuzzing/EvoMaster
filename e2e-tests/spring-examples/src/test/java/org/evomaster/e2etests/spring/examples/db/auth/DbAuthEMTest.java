package org.evomaster.e2etests.spring.examples.db.auth;

import org.evomaster.core.problem.rest.HttpVerb;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.Solution;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Disabled("Strangely, this does timeout on CircleCI, but works just fine in all other contexts")
public class DbAuthEMTest extends DbAuthTestBase {

    @Test
    public void testRunEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "DbAuthEM",
                "org.bar.db.AuthEM",
                500,
                (args) -> {

                    args.add("--heuristicsForSQL");
                    args.add("true");
                    args.add("--generateSqlDataWithSearch");
                    args.add("true");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/db/auth/users", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 401, "/api/db/auth/projects", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/db/auth/projects", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/db/auth/projects", null);
                });
    }
}
