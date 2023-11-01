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

public class AdaptiveHypermutationEMTest extends SpringRPCTestBase {


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
                25_000,
                (args) -> {
                    args.add("--baseTaintAnalysisProbability");
                    args.add("0.9");

                    Solution<RPCIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    assertRPCEndpointResult(solution, HypermutationService.Iface.class.getName()+":lowWeightHighCoverage", RPCCallResultCategory.HANDLED.name());
                    assertAllContentInResponseForEndpoint(solution,HypermutationService.Iface.class.getName()+":lowWeightHighCoverage" , Arrays.asList("x1","x2","x3","x4","x5", "y", "z"));
                });
    }
}
