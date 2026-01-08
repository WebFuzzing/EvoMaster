package org.evomaster.e2etests.spring.rpc.examples.taintignorecase;

import com.foo.rpc.examples.spring.taintignorecase.TaintIgnoreCaseController;
import com.foo.rpc.examples.spring.taintignorecase.TaintIgnoreCaseService;
import org.evomaster.core.problem.rpc.RPCCallResultCategory;
import org.evomaster.core.problem.rpc.RPCIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.rpc.examples.SpringRPCTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TaintIgnoreCaseEMTest extends SpringRPCTestBase {


    @BeforeAll
    public static void initClass() throws Exception {
        SpringRPCTestBase.initClass(new TaintIgnoreCaseController());
    }


    @Test
    public void testRunEM() throws Throwable {
        runTestHandlingFlakyAndCompilation(
                "TaintIgnoreCaseEM",
                "org.bar.TaintIgnoreCaseEM",
                5000,
                (args) -> {
                    args.add("--baseTaintAnalysisProbability");
                    args.add("0.9");

                    Solution<RPCIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    assertRPCEndpointResult(solution, TaintIgnoreCaseService.Iface.class.getName()+":getIgnoreCase", RPCCallResultCategory.HANDLED.name());
                    assertContentInResponseForEndpoint(solution,TaintIgnoreCaseService.Iface.class.getName()+":getIgnoreCase" ,"a123B");
                });
    }
}
