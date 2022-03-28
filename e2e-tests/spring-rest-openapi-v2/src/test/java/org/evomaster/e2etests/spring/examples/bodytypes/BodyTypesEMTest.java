package org.evomaster.e2etests.spring.examples.bodytypes;

import com.foo.rest.examples.spring.bodytypes.BodyTypesController;
import org.evomaster.core.problem.rest.HttpVerb;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Created by arcuri82 on 07-Nov-18.
 */
public class BodyTypesEMTest extends SpringTestBase {

    @BeforeAll
    public static void initClass() throws Exception {

        BodyTypesController controller = new BodyTypesController();
        SpringTestBase.initClass(controller);
    }


    @Test
    public void testRunEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "BodyTypeEM",
                "org.BodyTypeEM",
                500,
                (args) -> {

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/bodytypes/x", "0");
                    assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/bodytypes/x", "1");
                    assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/bodytypes/y", "2");
                    assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/bodytypes/z", "3");
                    assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/bodytypes/k", "4");
                    assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/bodytypes/k", "5");

                    //XML giving a few issues, and not so important
                    //assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/bodytypes/x", "6");

                    assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/bodytypes/q", "7");

                    //Looks like SpringFox or Swagger has bug in which this endpoint does not
                    //appear in the schema. But maybe in 3.0 could be expressed?
                    // Plus looks like issues in handling on Spring side
                    //TODO check with new SpringDoc in V3
                    //assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/bodytypes/q", "8");

                     assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/bodytypes/r", "9");
                     assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/bodytypes/t", "10");
                }
        );
    }
}