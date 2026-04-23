package org.evomaster.e2etests.spring.h2.columntypes;

import com.foo.spring.rest.h2.columntypes.H2ColumnTypesController;
import org.evomaster.core.problem.rest.data.HttpVerb;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.h2.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class H2ColumnTypesEMTest extends SpringTestBase {

    @BeforeAll
    public static void initClass() throws Exception {

        SpringTestBase.initClass(new H2ColumnTypesController());
    }

    @Test
    public void testRunEM() throws Throwable {

        defaultSeed = 67; //otherwise compilation error due to \\$

        runTestHandlingFlakyAndCompilation(
                "H2ColumnTypesEM",
                "com.foo.spring.rest.h2.columntypes.H2ColumnTypesEvoMaster",
                1000,
                (args) -> {
                    args.add("--enableWeightBasedMutationRateSelectionForGene");
                    args.add("false");

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

                    assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/h2/intervaltypes", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/h2/intervaltypes", null);

                    assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/h2/varcharignorecasetype", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/h2/varcharignorecasetype", null);

                    assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/h2/javaobjecttypes", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/h2/javaobjecttypes", null);

                    assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/h2/geometrytypes", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/h2/geometrytypes", null);

                    assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/h2/enumtype", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/h2/enumtype", null);

                    assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/h2/createtypeasenum", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/h2/createtypeasenum", null);

                    assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/h2/arraytypes", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/h2/arraytypes", null);
                });
    }
}
