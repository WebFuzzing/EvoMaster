package org.evomaster.e2etests.spring.examples.taint;

import com.foo.rest.examples.spring.taint.TaintController;
import org.evomaster.core.problem.rest.HttpVerb;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by arcuri82 on 06-Sep-19.
 */
public class TaintEMTest extends SpringTestBase {

    @BeforeAll
    public static void initClass() throws Exception {

        SpringTestBase.initClass(new TaintController());
    }

    @Test
    public void testRunEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "TaintEM",
                "org.bar.TaintEM",
                5000,
                (args) -> {

                    args.add("--baseTaintAnalysisProbability");
                    args.add("0.9");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/taint/integer","integer");
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/taint/date","date");
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/taint/constant","constant");
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/taint/thirdparty","thirdparty");
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/taint/collection","collection");
                });
    }
}