package org.evomaster.e2etests.spring.examples.taintMulti;

import com.foo.rest.examples.spring.taintMulti.TaintMultiController;
import org.evomaster.core.problem.rest.data.HttpVerb;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.evomaster.ci.utils.CIUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by arcuri82 on 06-Sep-19.
 */
public class TaintMultiEMTest extends SpringTestBase {

    @BeforeAll
    public static void initClass() throws Exception {

        SpringTestBase.initClass(new TaintMultiController());
    }

    @Test
    public void testDeterminism(){

        // CI: Java HotSpot(TM) 64-Bit Server VM warning: INFO: os::commit_memory(0x0000000616680000, 1243611136, 0) failed; error='Cannot allocate memory' (errno=12)
        CIUtils.skipIfOnCircleCI();

        defaultSeed = 15;

        runAndCheckDeterminism(5_000, (args) -> {
            initAndRun(args);
        });
    }

    @Test
    public void testRunEM() throws Throwable {

        CIUtils.skipIfOnCircleCI();

        defaultSeed = 123;

        runTestHandlingFlakyAndCompilation(
                "TaintMultiEM",
                "org.bar.TaintMultiEM",
                10_000,
                (args) -> {
                    args.add("--baseTaintAnalysisProbability");
                    args.add("0.9");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/taintMulti/separated/{x}/{date}", "separated");
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/taintMulti/together/{x}-{date}", "together");
                });
    }
}