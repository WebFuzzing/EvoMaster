package org.evomaster.e2etests.spring.asyncapi.kafka;

import com.foo.asyncapi.kafka.simple.SimpleKafkaController;
import org.evomaster.core.problem.asyncapi.data.AsyncApiCallResult;
import org.evomaster.core.problem.asyncapi.data.AsyncApiIndividual;
import org.evomaster.core.problem.asyncapi.data.AsyncApiOutcome;
import org.evomaster.core.problem.enterprise.ExperimentalFaultCategory;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.utils.AsyncApiTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A search over a service driven through Kafka: both operations reply with their declared
 * result, and a negative input makes each reply with a message the contract does not declare.
 */
public class SimpleKafkaEMTest extends AsyncApiTestBase {

    private static final SimpleKafkaController controller = new SimpleKafkaController();

    @BeforeAll
    public static void initClass() throws Exception {
        AsyncApiTestBase.initClass(controller);
    }

    @Test
    public void testRunEM() throws Throwable {

        //the generated suite is compiled and run, publishing with its own Kafka client rather than the driver
        runTestHandlingFlakyAndCompilation(
                "SimpleKafkaEM",
                "org.foo.asyncapi.SimpleKafkaEM",
                null,
                100,
                true,
                false,
                (args) -> {

                    //generous for a local round trip, but a GC pause on a loaded CI machine must not miss a reply
                    setOption(args, "asyncApiReplyTimeoutMs", "3000");

                    //the kill switch would stop the service's one consumer thread for good
                    setOption(args, "killSwitch", "false");

                    Solution<AsyncApiIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    assertReplyReached(solution, "foo", "result");
                    assertReplyReached(solution, "bar", "result");

                    assertRepliedWithUndeclaredMessage(solution, "foo");
                    assertRepliedWithUndeclaredMessage(solution, "bar");
                    assertNegativeXReached(solution, "foo");
                    assertNegativeXReached(solution, "bar");

                    assertEquals(0, countOutcome(solution, AsyncApiOutcome.NO_REPLY), "a promised reply never came");
                    assertEquals(0, countOutcome(solution, AsyncApiOutcome.PUBLISH_FAILED), "a message never left");

                    //both AsyncAPI fault categories are experimental, and off here
                    assertTrue(faultsOf(solution).isEmpty(), "faults were reported: " + faultsOf(solution));
                },
                5);
    }

    /**
     * That an undeclared reply came from a negative x, and not only from a request the service
     * could not read, which is answered the same way.
     */
    private void assertNegativeXReached(Solution<AsyncApiIndividual> solution, String operation) {
        boolean reached = resultsOf(solution, operation).stream()
                .map(AsyncApiCallResult::getReplyPayload)
                .anyMatch(payload -> payload != null && payload.contains("x must not be negative"));
        assertTrue(reached, "'" + operation + "' never replied for a negative x");
    }

    /**
     * The no-reply oracle, end to end. With replies disabled in the service, every message goes
     * unanswered, and that is reported as a fault once experimental oracles are asked for.
     */
    @Test
    public void testAPromisedReplyThatNeverArrivesIsAFault() throws Throwable {
        controller.setRepliesEnabled(false);
        try {

            runTestHandlingFlaky(
                    "SimpleKafkaNoReplyEM",
                    "org.foo.asyncapi.SimpleKafkaNoReplyEM",
                    30,
                    false,
                    (args) -> {

                        //replies are disabled, so this timeout does not decide the outcome
                        setOption(args, "asyncApiReplyTimeoutMs", "250");
                        setOption(args, "killSwitch", "false");
                        setOption(args, "useExperimentalOracles", "true");

                        Solution<AsyncApiIndividual> solution = initAndRun(args);

                        assertTrue(countOutcome(solution, AsyncApiOutcome.NO_REPLY) > 0,
                                "every message was answered in time, so the oracle had nothing to find");
                        assertTrue(faultsOf(solution).contains(ExperimentalFaultCategory.ASYNCAPI_NO_REPLY),
                                "the unanswered messages were not reported as a fault: " + faultsOf(solution));
                    },
                    3);
        } finally {
            controller.setRepliesEnabled(true);
        }
    }
}
