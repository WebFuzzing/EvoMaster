package org.evomaster.e2etests.spring.examples.adaptivehypermutation;

import com.foo.rest.examples.spring.adaptivehypermutation.AHypermutationRestController;
import org.evomaster.core.problem.rest.HttpVerb;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;


public class AHypermutationBaseTest extends AHypermuationTestBase {

    @Test
    public void testRunMIO() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "TestAHW",
                "org.adaptivehypermuation.BaseTest",
                50_000,
                true,
                (args) -> {

                    args.add("--probOfArchiveMutation");
                    args.add("0.0");

                    args.add("--weightBasedMutationRate");
                    args.add("false");

                    args.add("--adaptiveGeneSelectionMethod");
                    args.add("NONE");

                    args.add("--archiveGeneMutation");
                    args.add("NONE");

                    args.add("--enableTrackEvaluatedIndividual");
                    args.add("false");

                    Solution<RestIndividual> solution = initAndRun(args);
                    int count = 0;
                    List<String> msg = new ArrayList<>();
                    count = countExpected(solution, HttpVerb.POST, 200, "/api/foos/{x}", "B0", count, msg);
                    count = countExpected(solution, HttpVerb.POST, 200, "/api/foos/{x}", "B1", count, msg);
                    count = countExpected(solution, HttpVerb.POST, 200, "/api/foos/{x}", "B2", count, msg);
                    count = countExpected(solution, HttpVerb.POST, 200, "/api/foos/{x}", "B3", count, msg);
                    count = countExpected(solution, HttpVerb.POST, 200, "/api/foos/{x}", "B4", count, msg);
                    count = countExpected(solution, HttpVerb.POST, 200, "/api/foos/{x}", "B5", count, msg);
                    count = countExpected(solution, HttpVerb.POST, 200, "/api/foos/{x}", "B6", count, msg);

                    assertTrue(count < 3, String.join("\n", msg));
                }, 30);
    }


    @BeforeAll
    public static void initClass() throws Exception {
        SpringTestBase.initClass(new AHypermutationRestController(Arrays.asList("/api/bars/{a}")));
    }
}
