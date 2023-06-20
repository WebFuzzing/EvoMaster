package org.evomaster.e2etests.spring.rpc.grpc.examples.branches;

import com.foo.rpc.grpc.examples.spring.branches.BranchGRPCServiceController;
import org.evomaster.core.problem.rpc.RPCIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.rpc.grpc.examples.GRPCServerTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.assertTrue;

public class BranchesGRPCEMTest extends GRPCServerTestBase {

    @BeforeAll
    public static void initClass() throws Exception {

        BranchGRPCServiceController controller = new BranchGRPCServiceController();
        GRPCServerTestBase.initClass(controller);
    }

//    @Test
//    public void testRunEM() throws Throwable {
//
//        runTestHandlingFlakyAndCompilation(
//                "BranchesGRPCEM",
//                "org.foo.grpc.BranchesEM",
//                5000,
//                (args) -> {
//
//                    Solution<RPCIndividual> solution = initAndRun(args);
//
//                    assertTrue(solution.getIndividuals().size() >= 1);
//
//                });
//    }
}
