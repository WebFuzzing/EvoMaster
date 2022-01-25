package org.evomaster.e2etests.spring.rpc.examples.taint;

import com.foo.rpc.examples.spring.taint.TaintController;
import com.foo.rpc.examples.spring.taint.TaintService;
import org.evomaster.core.problem.rpc.RPCCallResultCategory;
import org.evomaster.core.problem.rpc.RPCIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.rpc.examples.SpringRPCTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TaintEMTest extends SpringRPCTestBase {


    @BeforeAll
    public static void initClass() throws Exception {
        SpringRPCTestBase.initClass(new TaintController());
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

                    Solution<RPCIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);
                    assertRPCEndpointResult(solution, TaintService.Iface.class.getName()+":getInteger", RPCCallResultCategory.HANDLED.name());
                    assertContentInResponseForEndpoint(solution, TaintService.Iface.class.getName()+":getInteger", "integer");
                    assertRPCEndpointResult(solution, TaintService.Iface.class.getName()+":getDate", RPCCallResultCategory.HANDLED.name());
                    assertContentInResponseForEndpoint(solution, TaintService.Iface.class.getName()+":getDate", "date");
                    assertRPCEndpointResult(solution, TaintService.Iface.class.getName()+":getConstant", RPCCallResultCategory.HANDLED.name());
                    assertContentInResponseForEndpoint(solution, TaintService.Iface.class.getName()+":getConstant", "constant OK");
                    assertRPCEndpointResult(solution, TaintService.Iface.class.getName()+":getThirdParty", RPCCallResultCategory.HANDLED.name());
                    assertContentInResponseForEndpoint(solution, TaintService.Iface.class.getName()+":getThirdParty", "thirdparty OK");
                    assertRPCEndpointResult(solution, TaintService.Iface.class.getName()+":getCollection", RPCCallResultCategory.HANDLED.name());
                    assertContentInResponseForEndpoint(solution, TaintService.Iface.class.getName()+":getCollection", "collection OK");
                });
    }
}
