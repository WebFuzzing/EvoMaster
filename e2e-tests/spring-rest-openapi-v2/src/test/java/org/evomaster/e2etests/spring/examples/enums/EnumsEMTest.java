package org.evomaster.e2etests.spring.examples.enums;

import com.foo.rest.examples.spring.enums.EnumsController;
import org.evomaster.core.Main;
import org.evomaster.core.problem.rest.HttpVerb;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.core.search.service.Statistics;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class EnumsEMTest extends SpringTestBase {

    @BeforeAll
    public static void initClass() throws Exception {

        SpringTestBase.initClass(new EnumsController());
    }

    @Test
    public void testRunEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "EnumEM",
                "org.bar.EnumEM",
                50,
                (args) -> {
                    Solution<RestIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/enums/{target}", "0");
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/enums/{target}", "1");

                    int last = Integer.parseInt(findValueOfItemWithKeyInStats(solution, Statistics.LAST_ACTION_IMPROVEMENT));
                    int total = Integer.parseInt(findValueOfItemWithKeyInStats(solution, Statistics.EVALUATED_ACTIONS));

                    assertTrue(last < total);
                });
    }
}
