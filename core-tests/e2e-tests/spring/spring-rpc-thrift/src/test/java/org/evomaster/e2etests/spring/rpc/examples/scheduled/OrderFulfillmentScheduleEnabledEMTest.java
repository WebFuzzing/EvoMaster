package org.evomaster.e2etests.spring.rpc.examples.scheduled;

import com.foo.rpc.examples.spring.scheduled.InventorySnapshotCache;
import com.foo.rpc.examples.spring.scheduled.OrderFulfillmentNoSeedController;
import com.foo.rpc.examples.spring.scheduled.OrderFulfillmentServiceImp;
import org.evomaster.client.java.instrumentation.staticstate.UnitsInfoRecorder;
import org.evomaster.core.EMConfig;
import org.evomaster.e2etests.spring.rpc.examples.SpringRPCTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.config.ScheduledTaskHolder;
import org.springframework.scheduling.support.ScheduledMethodRunnable;

import static org.junit.jupiter.api.Assertions.*;

public class OrderFulfillmentScheduleEnabledEMTest extends SpringRPCTestBase {

    private static ScheduleController scheduleController;

    @BeforeAll
    public static void initClass() throws Exception {
        EMConfig config = new EMConfig();
        config.setInstrumentMR_SCHEDULE(true);
        scheduleController = new ScheduleController();
        SpringRPCTestBase.initClass(scheduleController, config);
    }

    @Test
    public void testDiscoversPreservesAndInvokesSpringScheduledTask() throws Exception {
        assertTrue(UnitsInfoRecorder.getInstance().getScheduledMethods().contains(
                "com.foo.rpc.examples.spring.scheduled.OrderFulfillmentServiceImp#refreshInventorySnapshot()V"));

        Scheduled annotation = OrderFulfillmentServiceImp.class
                .getMethod("refreshInventorySnapshot").getAnnotation(Scheduled.class);
        assertNotNull(annotation);
        assertEquals(3_600_000L, annotation.fixedDelay());

        ScheduledTaskHolder holder = scheduleController.getBean(ScheduledTaskHolder.class);
        ScheduledMethodRunnable runnable = holder.getScheduledTasks().stream()
                .map(task -> task.getTask().getRunnable())
                .filter(task -> task instanceof ScheduledMethodRunnable)
                .map(task -> (ScheduledMethodRunnable) task)
                .filter(task -> task.getMethod().getName().equals("refreshInventorySnapshot"))
                .findFirst().orElseThrow(() -> new AssertionError("Spring did not register the scheduled task"));

        InventorySnapshotCache cache = scheduleController.getBean(InventorySnapshotCache.class);
        int before = cache.getRefreshCount();
        runnable.run();
        assertEquals(before + 1, cache.getRefreshCount());
    }

    private static class ScheduleController extends OrderFulfillmentNoSeedController {
        <T> T getBean(Class<T> type) {
            return ctx.getBean(type);
        }
    }
}
