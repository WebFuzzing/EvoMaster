package org.evomaster.e2etests.spring.examples.db.auth;

import org.evomaster.core.problem.rest.data.HttpVerb;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertTrue;

//@Disabled("Strangely, this does timeout on CircleCI, but works just fine in all other contexts")
public class DbAuthEMTest extends DbAuthTestBase {

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void testRunEM(boolean heuristicsForSQLAdvanced) throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "DbAuthEM",
                "org.bar.db.AuthEM" + (heuristicsForSQLAdvanced ? "Complete" : "Partial"),
                500,
                (args) -> {
                    setOption(args, "heuristicsForSQL", "true");
                    setOption(args, "generateSqlDataWithSearch", "true");
                    setOption(args, "heuristicsForSQLAdvanced", heuristicsForSQLAdvanced ? "true" : "false");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/db/auth/users", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 401, "/api/db/auth/projects", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/db/auth/projects", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/db/auth/projects", null);
                });
    }
}
