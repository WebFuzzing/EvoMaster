package org.evomaster.e2etests.spring.examples.db.preparedstatement;

import org.evomaster.core.EMConfig;
import org.evomaster.core.problem.rest.HttpVerb;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.Solution;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class PreparedStatementEMTest extends PreparedStatementTestBase {


    @Test
    public void testRunEM() throws Throwable {

        final String outputFolder = "PreparedStatement";
        final String outputTestName = "org.bar.db.PreparedStatement";

        runTestHandlingFlakyAndCompilation(
                outputFolder,
                outputTestName,
                100,
                (args) -> {

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/db/preparedStatement/{integerValue}/{stringValue}/{booleanValue}", null);
                    assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/db/preparedStatement", null);

                });
    }
}
