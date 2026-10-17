package org.evomaster.e2etests.spring.rpc.examples.scheduled;

import com.foo.rpc.examples.spring.scheduled.OrderFulfillmentNoSeedController;
import com.foo.rpc.examples.spring.scheduled.OrderFulfillmentServiceImp;
import org.evomaster.client.java.instrumentation.staticstate.UnitsInfoRecorder;
import org.evomaster.core.problem.rpc.RPCIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.rpc.examples.SpringRPCTestBase;
import org.junit.Ignore;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;

public class OrderFulfillmentNoSeedEMTest extends SpringRPCTestBase {

    @BeforeAll
    public static void initClass() throws Exception {
        SpringRPCTestBase.initClass(new OrderFulfillmentNoSeedController());
    }

    @Test
    public void testAutomaticallyDiscoversScheduledMethodAtStartup() throws Exception {
        assertScheduledMethodDiscovered();
        Scheduled annotation = OrderFulfillmentServiceImp.class
                .getMethod("refreshInventorySnapshot").getAnnotation(Scheduled.class);
        assertNull(annotation, "Scheduling annotations should be removed by default");
    }

    @Ignore
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

                    assertScheduledMethodDiscovered();

                    boolean hasScheduleOnlyTest = solution.getIndividuals().stream().anyMatch(s ->
                            s.getIndividual().seeMainExecutableActions().isEmpty()
                                    && !s.getIndividual().seeScheduleTaskActions().isEmpty()
                    );

                    assertTrue(hasScheduleOnlyTest, "Expected one generated test case with only a schedule task");
                });
    }

    private void assertScheduledMethodDiscovered() {
        String methodId = "com.foo.rpc.examples.spring.scheduled.OrderFulfillmentServiceImp"
                + "#refreshInventorySnapshot()V";
        assertTrue(UnitsInfoRecorder.getInstance().getScheduledMethods().contains(methodId),
                () -> "Expected automatic discovery of " + methodId
                        + "; discovered: " + UnitsInfoRecorder.getInstance().getScheduledMethods());
    }
}
