package org.evomaster.e2etests.spring.examples.db.jpa;

import com.foo.rest.examples.spring.db.jpa.EntityJPAController;
import org.evomaster.core.problem.rest.data.HttpVerb;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class EntityJPAEMTest extends SpringTestBase {

    @BeforeAll
    public static void initClass() throws Exception {

        SpringTestBase.initClass(new EntityJPAController());
    }

    @Test
    public void testFailRunEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "EntityJPAEM_Fail",
                "org.bar.db.FailEntityJPAEM",
                20,
                (args) -> {

                    args.add("--useExtraSqlDbConstraintsProbability");
                    args.add("0.0");


                    Solution<RestIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    //should be nearly impossible to get 30 values with not a single NULL
                    assertNone(solution,HttpVerb.PUT,200);
                });
    }

    @Test
    public void testOkRunEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "EntityJPAEM_OK",
                "org.bar.db.OkEntityJPAEM",
                20,
                (args) -> {

                    args.add("--useExtraSqlDbConstraintsProbability");
                    args.add("1.0");


                    Solution<RestIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    assertHasAtLeastOne(solution, HttpVerb.PUT, 200, "/api/db/jpa", "OK");
                    //assertNone(solution,HttpVerb.PUT,500);

                });
    }

}
