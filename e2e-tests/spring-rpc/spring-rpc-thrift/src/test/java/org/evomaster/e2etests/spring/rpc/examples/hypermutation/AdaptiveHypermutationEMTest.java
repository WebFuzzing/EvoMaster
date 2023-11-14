package org.evomaster.e2etests.spring.rpc.examples.hypermutation;

import com.foo.rpc.examples.spring.hypermutation.HypermutationController;
import com.foo.rpc.examples.spring.hypermutation.HypermutationService;
import org.evomaster.core.problem.rpc.RPCCallResultCategory;
import org.evomaster.core.problem.rpc.RPCIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.rpc.examples.SpringRPCTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class AdaptiveHypermutationEMTest extends RPCHypermutationTestBase {


    @BeforeAll
    public static void initClass() throws Exception {
        SpringRPCTestBase.initClass(new HypermutationController(new HashMap<String, List<String>>(){{
            put(HypermutationService.Iface.class.getName(), new ArrayList<String>(){{add("differentWeight");}});
        }}));
    }

    @Test
    public void testRunEM() throws Throwable {
        runTestHandlingFlakyAndCompilation(
                "AdaptiveHypermutationEM",
                "org.bar.AdaptiveHypermutationEM",
                500,
                (args) -> {

                    args.add("--weightBasedMutationRate");
                    args.add("true");

                    args.add("--probOfArchiveMutation");
                    args.add("1.0");
                    args.add("--adaptiveGeneSelectionMethod");
                    args.add("APPROACH_IMPACT");
                    args.add("--archiveGeneMutation");
                    args.add("SPECIFIED_WITH_SPECIFIC_TARGETS");
                    args.add("--enableTrackEvaluatedIndividual");
                    args.add("true");
                    args.add("--startingPerOfGenesToMutate");
                    args.add("0.1");


                    args.add("--probOfRandomSampling");
                    args.add("0.0");
                    //minimization loses impact info
                    args.add("--minimize");
                    args.add("false");

                    Solution<RPCIndividual> solution = initAndRun(args);

                    boolean ok = check(solution, "lowWeightHighCoverage",1);
                    assertTrue(ok);
                });
    }
}
