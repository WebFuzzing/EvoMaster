package org.evomaster.e2etests.spring.rpc.examples.scheduled;

import com.foo.rpc.examples.spring.scheduled.OrderFulfillmentController;
import com.foo.rpc.examples.spring.scheduled.OrderFulfillmentService;
import org.evomaster.core.problem.rpc.RPCIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.rpc.examples.SpringRPCTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class OrderFulfillmentEMTest extends SpringRPCTestBase {

    @BeforeAll
    public static void initClass() throws Exception {
        SpringRPCTestBase.initClass(new OrderFulfillmentController());
    }

    @Test
    public void testRunEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "OrderFulfillmentRPCEM",
                "org.bar.OrderFulfillmentRPCEM",
                10,
                (args) -> {
                    args.add("--seedTestCases");
                    args.add("true");

                    args.add("--enableCustomizedMethodForMockObjectHandling");
                    args.add("true");

                    setOption(args, "enableCustomizedMethodForScheduleTaskHandling", "true");
                    setOption(args, "probOfSamplingScheduleTask", "1.0");
                    setOption(args, "minimize", "false");
                    setOption(args, "security", "false");

                    Solution<RPCIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);
                    String endpoint = OrderFulfillmentService.Iface.class.getName() + ":evaluateOrder";
                    assertContentInResponseForEndpoint(solution, endpoint, "APPROVED_PRIORITY");
                    assertContentInResponseForEndpoint(solution, endpoint, "APPROVED_STANDARD");
                    assertContentInResponseForEndpoint(solution, endpoint, "OUT_OF_STOCK");
                    assertContentInResponseForEndpoint(solution, endpoint, "PAYMENT_DECLINED");
                    assertContentInResponseForEndpoint(solution, endpoint, "PENDING_INVENTORY_REFRESH");
                });
    }
}
