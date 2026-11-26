package org.evomaster.e2etests.spring.asyncapi.kafka;

import com.foo.asyncapi.kafka.listener.ListenerKafkaController;
import org.evomaster.client.java.controller.api.dto.problem.asyncapi.AsyncApiReplyDto;
import org.evomaster.core.EMConfig;
import org.evomaster.core.problem.asyncapi.data.AsyncApiIndividual;
import org.evomaster.core.problem.asyncapi.data.AsyncApiOutcome;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.utils.AsyncApiTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The listener service with each message let go once its listener has returned, as told by
 * Spring Kafka's listener container polling again for more. The container polls through the
 * Consumer interface, so this is what the Kafka instrumentation sees of a {@code @KafkaListener}.
 *
 * A class of its own, rather than one more test in {@link ListenerKafkaEMTest}: the Kafka
 * replacements can only be asked for when {@link #initClass()} starts, and instruments, the
 * service. Each test class runs in a JVM of its own in this module.
 */
public class ListenerKafkaHandlerReturnEMTest extends AsyncApiTestBase {

    @BeforeAll
    public static void initClass() throws Exception {

        EMConfig config = new EMConfig();
        config.setInstrumentMR_KAFKA(true);

        AsyncApiTestBase.initClass(new ListenerKafkaController(), config);
    }

    /**
     * Not asserted: the express dispatch. The listener hands it to an executor and returns at
     * once, so the wait ends before it is booked: the blind spot of this completion.
     */
    @Test
    public void testRunEMWithHandlerReturn() throws Throwable {

        runTestHandlingFlaky(
                "ListenerKafkaHandlerReturnEM",
                "org.foo.asyncapi.ListenerKafkaHandlerReturnEM",
                50,
                false,
                (args) -> {

                    /*
                        A cap far longer than the listener ever needs: a wait that went to it
                        would mean the end of the handler was never seen.
                     */
                    setOption(args, "asyncApiSettleMs", "5000");
                    setOption(args, "asyncApiCompletion", "HANDLER_RETURN");
                    setOption(args, "instrumentMR_KAFKA", "true");

                    Solution<AsyncApiIndividual> solution = initAndRun(args);

                    assertTrue(countOutcome(solution, AsyncApiOutcome.PUBLISHED) > 0, "nothing was published");
                    assertEquals(0, countOutcome(solution, AsyncApiOutcome.PUBLISH_FAILED), "a message never left");
                    assertEverySettled(solution);

                    assertTrue(countSettleEnd(solution, AsyncApiReplyDto.SettleEndReason.HANDLER_RETURN) > 0,
                            "no wait ended on the listener returning");
                    assertEquals(0, countSettleEnd(solution, AsyncApiReplyDto.SettleEndReason.DEADLINE),
                            "a wait went to the deadline, so the listener was never seen returning");
                    assertEquals(0, countSettleEnd(solution, AsyncApiReplyDto.SettleEndReason.INTERRUPTED),
                            "a wait was interrupted");

                    //the service publishes nothing, so nothing is recorded as emitted
                    assertTrue(emittedMessagesOf(solution).isEmpty(), "emitted: " + emittedMessagesOf(solution));
                },
                5);
    }
}
