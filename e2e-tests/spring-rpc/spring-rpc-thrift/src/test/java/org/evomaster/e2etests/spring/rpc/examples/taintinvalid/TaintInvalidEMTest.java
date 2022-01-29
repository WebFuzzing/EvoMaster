package org.evomaster.e2etests.spring.rpc.examples.taintinvalid;

import com.foo.rpc.examples.spring.taintinvalid.TaintInvalidController;
import com.foo.rpc.examples.spring.taintinvalid.TaintInvalidService;
import com.foo.rpc.examples.spring.taintinvalid.TaintInvalidServiceImp;
import org.evomaster.core.problem.rpc.RPCCallResultCategory;
import org.evomaster.core.problem.rpc.RPCIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.rpc.examples.SpringRPCTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TaintInvalidEMTest extends SpringRPCTestBase {


    @BeforeAll
    public static void initClass() throws Exception {
        SpringRPCTestBase.initClass(new TaintInvalidController());
    }


    @Test
    public void testRunEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "TaintInvalidEM",
                "org.bar.TaintInvalidEM",
                1000,
                (args) -> {
                    args.add("--baseTaintAnalysisProbability");
                    args.add("0.9");

                    Solution<RPCIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    assertRPCEndpointResult(solution, TaintInvalidService.Iface.class.getName()+":get", RPCCallResultCategory.HANDLED.name());
                    assertContentInResponseForEndpoint(solution,TaintInvalidService.Iface.class.getName()+":get" ,"foo");
                    assertAnyContentInResponseForEndpoint(solution,TaintInvalidService.Iface.class.getName()+":get" , TaintInvalidServiceImp.list);
                });
    }
}
