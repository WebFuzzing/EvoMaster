package org.evomaster.e2etests.spring.examples.db.base;

import org.evomaster.core.problem.rest.HttpVerb;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.Solution;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class DbBaseEMTest extends DbBaseTestBase {


    @Test
    public void testRunEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "DbBaseEM",
                "org.bar.db.BaseEM",
                10_000,
                (args) -> {

                    args.add("--heuristicsForSQL");
                    args.add("true");
                    args.add("--generateSqlDataWithSearch");
                    args.add("false");

                    //FIXME: need to study and fix the side effects of Taint here
                    args.add("--baseTaintAnalysisProbability");
                    args.add("0.0");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/db/base/entitiesByName/{name}", "");
                });
    }
}
