package org.evomaster.e2etests.spring.examples.taintInvalid;

import com.foo.rest.examples.spring.taintInvalid.TaintInvalidController;
import org.evomaster.core.problem.rest.data.HttpVerb;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.evomaster.ci.utils.CIUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by arcuri82 on 06-Sep-19.
 */
public class TaintInvalidEMTest extends SpringTestBase {

    @BeforeAll
    public static void initClass() throws Exception {

        SpringTestBase.initClass(new TaintInvalidController());
    }

    @Test
    public void testRunEM() throws Throwable {

        CIUtils.skipIfOnCircleCI();

        runTestHandlingFlakyAndCompilation(
                "TaintInvalidEM",
                "org.bar.TaintInvalidEM",
                1000,
                (args) -> {
                    args.add("--baseTaintAnalysisProbability");
                    args.add("0.9");
                    args.add("--addPreDefinedTests");
                    args.add("false");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/taintInvalid/{x}","foo");
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/taintInvalid/{x}","bar");
                });
    }
}