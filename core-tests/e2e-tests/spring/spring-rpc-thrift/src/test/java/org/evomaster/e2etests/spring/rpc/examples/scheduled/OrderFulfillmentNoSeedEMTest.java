package org.evomaster.e2etests.spring.rpc.examples.scheduled;

import com.foo.rpc.examples.spring.scheduled.OrderFulfillmentNoSeedController;
import org.evomaster.core.problem.rpc.RPCIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.rpc.examples.SpringRPCTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class OrderFulfillmentNoSeedEMTest extends SpringRPCTestBase {

    @BeforeAll
    public static void initClass() throws Exception {
        SpringRPCTestBase.initClass(new OrderFulfillmentNoSeedController());
    }

    @Test
    public void testRunEMWithoutSeedHasScheduleOnlyTest() throws Throwable {

        runTestHandlingFlaky(
                "OrderFulfillmentNoSeedRPCEM",
                "org.bar.OrderFulfillmentNoSeedRPCEM",
                100,
                false,
                (args) -> {

                    setOption(args, "seedTestCases", "false");
                    setOption(args, "enableCustomizedMethodForScheduleTaskHandling", "true");
                    setOption(args, "probOfSamplingScheduleTask", "1.0");
                    setOption(args, "minimize", "false");
                    setOption(args, "security", "false");

                    Solution<RPCIndividual> solution = initAndRun(args);

                    boolean hasScheduleOnlyTest = solution.getIndividuals().stream().anyMatch(s ->
                            s.getIndividual().seeMainExecutableActions().isEmpty()
                                    && s.getIndividual().seeScheduleTaskActions().size() == 1
                    );

                    assertTrue(hasScheduleOnlyTest, "Expected one generated test case with only a schedule task");
                });
    }
}
