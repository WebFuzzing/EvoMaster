package org.evomaster.e2etests.spring.mysql.entity;

import com.foo.spring.rest.mysql.entity.SpringRestMyEntityController;
import org.evomaster.core.problem.rest.data.HttpVerb;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class MyEntityEMTest extends SpringTestBase {

    @BeforeAll
    public static void initClass() throws Exception {
        SpringTestBase.initClass(new SpringRestMyEntityController());
    }

    @Test
    public void testRunEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "MyEntityEM",
                "com.foo.spring.rest.mysql.MyEntityEvoMaster",
                100,
                (args) -> {

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/myentities/{id}", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/myentities/{id}", null);

                });
    }
}
