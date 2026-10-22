package org.evomaster.e2etests.spring.asyncapi.kafka;

import com.foo.asyncapi.ncs.NcsKafkaController;
import org.evomaster.client.java.controller.api.dto.EmittedMessageDto;
import org.evomaster.client.java.controller.api.dto.problem.asyncapi.AsyncApiReplyDto;
import org.evomaster.core.EMConfig;
import org.evomaster.core.problem.asyncapi.data.AsyncApiIndividual;
import org.evomaster.core.problem.asyncapi.data.AsyncApiOutcome;
import org.evomaster.core.problem.enterprise.ExperimentalFaultCategory;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.utils.AsyncApiTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A search over NCS driven through Kafka: every operation answers, and where the contract
 * declares a result and an error, the search reaches both.
 */
public class NcsKafkaEMTest extends AsyncApiTestBase {

    private static final List<String> OPERATIONS =
            Arrays.asList("checkTriangle", "bessj", "expint", "fisher", "gammq", "remainder");

    /**
     * The suffix of the topics NCS answers on; the requests go to topics of their own.
     */
    private static final String REPLY_TOPIC_SUFFIX = ".reply";

    @BeforeAll
    public static void initClass() throws Exception {

        /*
            The service is instrumented once, when started here. The Kafka replacements are off
            by default and the handler-return run needs them; the other runs ignore what they
            record.
         */
        EMConfig config = new EMConfig();
        config.setInstrumentMR_KAFKA(true);

        AsyncApiTestBase.initClass(new NcsKafkaController(), config);
    }

    @Test
    public void testRunEM() throws Throwable {

        /*
            The generated suite is compiled and run, so this covers the writer as well as the
            search: the tests publish with a Kafka client of their own, written from the
            contract, and never go through the driver.
         */
        runTestHandlingFlakyAndCompilation(
                "NcsKafkaEM",
                "org.foo.asyncapi.NcsKafkaEM",
                null,
                300,
                true,
                false,
                (args) -> {

                    /*
                        The broker is local and the service answers in milliseconds. Left at its
                        default, one stalled message would eat a large share of the budget for
                        this test; a second is generous for the round trip, but not for a loaded
                        CI machine, where a garbage collection in the SUT can cost more than
                        that, and this test asserts that no reply was missed.
                     */
                    args.add("--asyncApiReplyTimeoutMs");
                    args.add("3000");

                    /*
                        The kill switch would stop the service's one consumer thread for good. It
                        is off by default for AsyncAPI; a missed reply would fail NO_REPLY below.
                     */

                    Solution<AsyncApiIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    for (String operation : OPERATIONS) {
                        assertReplied(solution, operation);
                    }

                    assertReplyReached(solution, "checkTriangle", "intResult");
                    assertReplyReached(solution, "bessj", "doubleResult");
                    assertReplyReached(solution, "remainder", "intResult");

                    /*
                        An error reply is reachable exactly where the schema leaves the input the
                        service rejects within what it allows. expint's x and gammq's a state
                        their constraint in prose rather than as a bound, and fisher's x carries
                        none at all, so the search is free to produce one.

                        remainder declares an error reply that nothing obeying the contract can
                        provoke: its bounds are exactly the range the service checks. That is a
                        property of the document, not a gap in the search. bessj's n is fenced
                        the same way, but its x is not, so an error there is reachable too --
                        rarely enough that this test does not ask for it.
                     */
                    assertReplyReached(solution, "expint", "doubleResult");
                    assertReplyReached(solution, "expint", "error");
                    assertReplyReached(solution, "gammq", "doubleResult");
                    assertReplyReached(solution, "gammq", "error");
                    assertReplyReached(solution, "fisher", "doubleResult");
                    assertReplyReached(solution, "fisher", "error");

                    /*
                        checkTriangle answers an edge that is not positive with a shape the
                        contract does not declare. Only the classifier can tell: the message
                        arrived, was correlated, and still means nothing to a client written
                        against the document.
                     */
                    assertRepliedWithUndeclaredMessage(solution, "checkTriangle");

                    assertEquals(0, countOutcome(solution, AsyncApiOutcome.NO_REPLY), "a promised reply never came");
                    assertEquals(0, countOutcome(solution, AsyncApiOutcome.PUBLISH_FAILED), "a message never left");

                    /*
                        Both AsyncAPI categories are experimental and the flag is off here, so
                        even the undeclared reply asserted above goes unreported. The other run
                        turns the flag on and checks the other side of that.
                     */
                    assertTrue(faultsOf(solution).isEmpty(), "faults were reported: " + faultsOf(solution));
                },
                5);
    }

    @Test
    public void testRunEMInJava() throws Throwable {

        /*
            The lines that publish are written in the language the suite is generated in, and
            only one of the two is exercised by the test above. Compiling and running the other
            is what catches emitted Java that does not compile.

            A short budget: what is under test here is the emitted Java, not the search.
         */
        runTestHandlingFlakyAndCompilation(
                "NcsKafkaJavaEM",
                "org.foo.asyncapi.NcsKafkaJavaEM",
                null,
                40,
                true,
                false,
                (args) -> {

                    setOption(args, "outputFormat", "JAVA_JUNIT_5");
                    setOption(args, "asyncApiReplyTimeoutMs", "3000");

                    Solution<AsyncApiIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);
                    assertTrue(countOutcome(solution, AsyncApiOutcome.REPLIED) > 0, "nothing was answered");
                },
                5);
    }

    /**
     * A search that keeps every action open for a while after the driver returns. The service
     * goes on answering, and every message is recorded as having been waited on.
     */
    @Test
    public void testRunEMWithASettleWindow() throws Throwable {

        runTestHandlingFlaky(
                "NcsKafkaSettleEM",
                "org.foo.asyncapi.NcsKafkaSettleEM",
                40,
                false,
                (args) -> {

                    setOption(args, "asyncApiReplyTimeoutMs", "3000");

                    //long enough to be noticed, short enough for the budget: every action takes at least this long
                    setOption(args, "asyncApiSettleMs", "200");

                    Solution<AsyncApiIndividual> solution = initAndRun(args);

                    assertTrue(countOutcome(solution, AsyncApiOutcome.REPLIED) > 0, "nothing was answered");
                    assertEquals(0, countOutcome(solution, AsyncApiOutcome.NO_REPLY), "a promised reply never came");
                    assertEquals(0, countOutcome(solution, AsyncApiOutcome.PUBLISH_FAILED), "a message never left");
                    assertEverySettled(solution);
                },
                3);
    }

    /**
     * The same, but each action ends once the service stops reaching new code. NCS handles a
     * message well within the window and then only polls lines already covered, so most waits
     * end early.
     */
    @Test
    public void testRunEMWithCoverageStable() throws Throwable {

        runTestHandlingFlaky(
                "NcsKafkaCoverageStableEM",
                "org.foo.asyncapi.NcsKafkaCoverageStableEM",
                40,
                false,
                (args) -> {

                    setOption(args, "asyncApiReplyTimeoutMs", "3000");

                    /*
                        A window far longer than NCS ever needs, and a stable period far shorter:
                        a wait that went to the deadline every time would mean stable coverage was
                        never detected.
                     */
                    setOption(args, "asyncApiSettleMs", "5000");
                    setOption(args, "asyncApiCompletion", "COVERAGE_STABLE");
                    setOption(args, "asyncApiStableMs", "100");

                    Solution<AsyncApiIndividual> solution = initAndRun(args);

                    assertTrue(countOutcome(solution, AsyncApiOutcome.REPLIED) > 0, "nothing was answered");
                    assertEquals(0, countOutcome(solution, AsyncApiOutcome.NO_REPLY), "a promised reply never came");
                    assertEquals(0, countOutcome(solution, AsyncApiOutcome.PUBLISH_FAILED), "a message never left");
                    assertEverySettled(solution);

                    assertTrue(countSettleEnd(solution, AsyncApiReplyDto.SettleEnd.COVERAGE_STABLE) > 0,
                            "no wait ended on stable coverage");
                    assertEquals(0, countSettleEnd(solution, AsyncApiReplyDto.SettleEnd.INTERRUPTED),
                            "a wait was interrupted");
                },
                3);
    }

    /**
     * The same, but each action ends once the service's consumer thread goes back to poll. NCS
     * polls with KafkaConsumer and answers with KafkaProducer, which the Kafka instrumentation
     * tracks, so every wait ends that way, and its replies are recorded on the action answered.
     */
    @Test
    public void testRunEMWithHandlerReturn() throws Throwable {

        runTestHandlingFlaky(
                "NcsKafkaHandlerReturnEM",
                "org.foo.asyncapi.NcsKafkaHandlerReturnEM",
                40,
                false,
                (args) -> {

                    setOption(args, "asyncApiReplyTimeoutMs", "3000");

                    /*
                        A window far longer than NCS ever needs: a wait that went to the deadline
                        would mean the end of the handler was never seen.
                     */
                    setOption(args, "asyncApiSettleMs", "5000");
                    setOption(args, "asyncApiCompletion", "HANDLER_RETURN");
                    setOption(args, "instrumentMR_KAFKA", "true");

                    Solution<AsyncApiIndividual> solution = initAndRun(args);

                    assertTrue(countOutcome(solution, AsyncApiOutcome.REPLIED) > 0, "nothing was answered");
                    assertEquals(0, countOutcome(solution, AsyncApiOutcome.NO_REPLY), "a promised reply never came");
                    assertEquals(0, countOutcome(solution, AsyncApiOutcome.PUBLISH_FAILED), "a message never left");
                    assertReplyReached(solution, "bessj", "doubleResult");
                    assertReplyReached(solution, "checkTriangle", "intResult");
                    assertEverySettled(solution);

                    assertTrue(countSettleEnd(solution, AsyncApiReplyDto.SettleEnd.HANDLER_RETURN) > 0,
                            "no wait ended on the handler returning");
                    assertEquals(0, countSettleEnd(solution, AsyncApiReplyDto.SettleEnd.DEADLINE),
                            "a wait went to the deadline, so the service was never seen done with a message");
                    assertEquals(0, countSettleEnd(solution, AsyncApiReplyDto.SettleEnd.INTERRUPTED),
                            "a wait was interrupted");

                    //only its replies: the driver's publishes, made in this JVM, are not the service's
                    List<EmittedMessageDto> emitted = emittedMessagesOf(solution);
                    assertFalse(emitted.isEmpty(), "nothing the service emitted was recorded");
                    List<String> notReplies = emitted.stream()
                            .map(m -> m.address)
                            .filter(a -> a == null || !a.endsWith(REPLY_TOPIC_SUFFIX))
                            .collect(Collectors.toList());
                    assertTrue(notReplies.isEmpty(), "recorded as emitted by the service: " + notReplies);
                },
                3);
    }

    /**
     * The no-reply oracle, end to end. Given a deadline no round trip through a broker can meet,
     * every message goes unanswered, and that is reported as a fault once experimental oracles
     * are asked for.
     */
    @Test
    public void testAPromisedReplyThatNeverArrivesIsAFault() throws Throwable {

        runTestHandlingFlaky(
                "NcsKafkaNoReplyEM",
                "org.foo.asyncapi.NcsKafkaNoReplyEM",
                30,
                false,
                (args) -> {

                    /*
                        Shorter than any round trip through a broker. The service answers as it
                        always does, just never in time, which is how an unanswered message tends
                        to look in practice. Nothing has to be broken on purpose for it.
                     */
                    args.add("--asyncApiReplyTimeoutMs");
                    args.add("1");

                    /*
                        Both AsyncAPI categories are experimental, so without this the outcome is
                        still reached and still a target, but nothing is reported as a fault.
                     */
                    args.add("--useExperimentalOracles");
                    args.add("true");

                    Solution<AsyncApiIndividual> solution = initAndRun(args);

                    assertTrue(countOutcome(solution, AsyncApiOutcome.NO_REPLY) > 0,
                            "every message was answered in time, so the oracle had nothing to find");
                    assertTrue(faultsOf(solution).contains(ExperimentalFaultCategory.ASYNCAPI_NO_REPLY),
                            "the unanswered messages were not reported as a fault: " + faultsOf(solution));
                },
                3);
    }
}
