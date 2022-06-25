package org.evomaster.e2etests.spring.h2.datatypes;

import com.foo.spring.rest.h2.datatypes.H2DataTypesAppController;
import org.evomaster.core.problem.rest.HttpVerb;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.h2.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class H2DataTypesEMTest extends SpringTestBase {

    @BeforeAll
    public static void initClass() throws Exception {

        SpringTestBase.initClass(new H2DataTypesAppController());
    }

    @Test
    public void testRunEM() throws Throwable {


        runTestHandlingFlakyAndCompilation(
                "H2DataTypesEM",
                "com.foo.spring.rest.h2.types.H2DataTypesEM_",
                500,
                (args) -> {

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/h2/charactertypes", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/h2/charactertypes", null);

                    assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/h2/charactervaryingtypes", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/h2/charactervaryingtypes", null);

                    assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/h2/characterlargeobjecttypes", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/h2/characterlargeobjecttypes", null);

                    assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/h2/binarytypes", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/h2/binarytypes", null);

                    assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/h2/numerictypes", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/h2/numerictypes", null);

                    assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/h2/datetimetypes", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/h2/datetimetypes", null);

                    assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/h2/jsontype", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/h2/jsontype", null);

                    assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/h2/uuidtype", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/h2/uuidtype", null);

                    //assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/h2/intervaltypes", null);
                    //assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/h2/intervaltypes", null);

                    assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/h2/varcharignorecasetype", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/h2/varcharignorecasetype", null);

                    assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/h2/javaobjecttypes", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/h2/javaobjecttypes   ", null);
                });
    }
}
