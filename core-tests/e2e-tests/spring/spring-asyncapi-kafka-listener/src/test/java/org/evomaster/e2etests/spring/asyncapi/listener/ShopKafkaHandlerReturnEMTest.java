package org.evomaster.e2etests.spring.asyncapi.listener;

import com.foo.asyncapi.ShopKafkaController;
import com.foo.asyncapi.shop.ShopTopics;
import com.google.inject.Injector;
import kotlin.Pair;
import org.evomaster.client.java.controller.api.dto.problem.asyncapi.AsyncApiReplyDto;
import org.evomaster.core.EMConfig;
import org.evomaster.core.problem.asyncapi.data.AsyncApiIndividual;
import org.evomaster.core.problem.asyncapi.data.AsyncApiOutcome;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.utils.AsyncApiTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The shop service with each message let go once its listener has returned, as told by Spring
 * Kafka's listener container polling again for more. The container polls through the Consumer
 * interface, so this is what the Kafka instrumentation sees of a {@code @KafkaListener}.
 *
 * A class of its own, rather than one more test in {@link ShopKafkaEMTest}: the Kafka
 * replacements can only be asked for when {@link #initClass()} starts, and instruments, the
 * service. Each test class runs in a JVM of its own in this module.
 */
public class ShopKafkaHandlerReturnEMTest extends AsyncApiTestBase {

    private static final String SUT_PACKAGE = "com.foo.asyncapi.shop.";

    private static final String EXPRESS_DISPATCH = SUT_PACKAGE + "ExpressDispatch";

    private static final String URGENT_ESCALATION = SUT_PACKAGE + "UrgentEscalation";

    private static final List<String> OPERATIONS = Arrays.asList("placeOrder", "requestInvoice", "adjustStock");

    /**
     * The topics the service publishes to.
     */
    private static final List<String> SERVICE_TOPICS = Arrays.asList(ShopTopics.ORDERS_CONFIRMED, ShopTopics.STOCK_CHANGES);

    @BeforeAll
    public static void initClass() throws Exception {

        EMConfig config = new EMConfig();
        config.setInstrumentMR_KAFKA(true);

        AsyncApiTestBase.initClass(new ShopKafkaController(), config);
    }

    /**
     * Express shipping and an urgent priority are reached as in {@link ShopKafkaEMTest#testRunEM()},
     * by taint analysis: the listener compares the payload's strings before it returns, so what
     * it compared them with is recorded within the window.
     *
     * Not asserted: the invoice. Its listener hands the rendering to an executor and returns at
     * once, so the wait ends before the invoice is rendered: the blind spot of this completion.
     */
    @Test
    public void testRunEMWithHandlerReturn() throws Throwable {

        runTestHandlingFlaky(
                "ShopKafkaHandlerReturnEM",
                "org.foo.asyncapi.ShopKafkaHandlerReturnEM",
                300,
                false,
                (args) -> {

                    /*
                        A cap far longer than a listener here ever needs: a wait that went to it
                        would mean the end of the handler was never seen.
                     */
                    setOption(args, "asyncApiSettleMs", "5000");
                    setOption(args, "asyncApiCompletion", "HANDLER_RETURN");
                    setOption(args, "instrumentMR_KAFKA", "true");

                    //as in ShopKafkaEMTest: more, shorter tests for the budget
                    setOption(args, "maxTestSize", "3");

                    Pair<Injector, Solution<AsyncApiIndividual>> run = initAndDebug(args);
                    Solution<AsyncApiIndividual> solution = run.getSecond();

                    assertTrue(countOutcome(solution, AsyncApiOutcome.PUBLISHED) > 0, "nothing was published");
                    assertEquals(0, countOutcome(solution, AsyncApiOutcome.PUBLISH_FAILED), "a message never left");
                    assertEverySettled(solution);

                    for (String operation : OPERATIONS) {
                        long ended = resultsOf(solution, operation).stream()
                                .filter(r -> r.getSettleEndedBy() == AsyncApiReplyDto.SettleEnd.HANDLER_RETURN)
                                .count();
                        assertTrue(ended > 0, "no wait on '" + operation + "' ended on its listener returning");
                    }
                    assertEquals(0, countSettleEnd(solution, AsyncApiReplyDto.SettleEnd.DEADLINE),
                            "a wait went to the deadline, so a listener was never seen returning");
                    assertEquals(0, countSettleEnd(solution, AsyncApiReplyDto.SettleEnd.INTERRUPTED),
                            "a wait was interrupted");

                    assertClassCovered(run, EXPRESS_DISPATCH);
                    assertClassCovered(run, URGENT_ESCALATION);

                    //its confirmations and stock changes; the driver's publishes, made in this JVM, are not the service's
                    List<String> emitted = emittedMessagesOf(solution).stream()
                            .map(m -> m.address)
                            .distinct()
                            .collect(Collectors.toList());
                    assertTrue(emitted.contains(ShopTopics.ORDERS_CONFIRMED), "emitted: " + emitted);
                    assertTrue(SERVICE_TOPICS.containsAll(emitted), "recorded as emitted by the service: " + emitted);
                },
                5);
    }
}
