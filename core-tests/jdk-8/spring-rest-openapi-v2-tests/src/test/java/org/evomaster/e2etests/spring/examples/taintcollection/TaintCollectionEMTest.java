package org.evomaster.e2etests.spring.examples.taintcollection;

import com.foo.rest.examples.spring.taintcollection.TaintCollectionController;
import org.evomaster.core.problem.rest.data.HttpVerb;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by arcuri82 on 06-Sep-19.
 */
public class TaintCollectionEMTest extends SpringTestBase {

    @BeforeAll
    public static void initClass() throws Exception {

        SpringTestBase.initClass(new TaintCollectionController());
    }

    @Test
    public void testRunEM() throws Throwable {

        defaultSeed = 0;

        runTestHandlingFlakyAndCompilation(
                "TaintCollectionEM",
                "org.bar.TaintCollectionEM",
                10_000, //TODO likely this can be reduced when supporting taint in sampling
                (args) -> {

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    String base = "/api/taintcollection/";

                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, base+"contains", "OK");
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, base+"containsAll", "OK");
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, base+"remove", "OK");
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, base+"removeAll", "OK");
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, base+"map/containsKey", "OK");
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, base+"map/containsValue", "OK");
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, base+"map/get", "OK");
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, base+"map/getOrDefault", "OK");
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, base+"map/remove", "OK");
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, base+"map/replace", "OK");
                });
    }
}