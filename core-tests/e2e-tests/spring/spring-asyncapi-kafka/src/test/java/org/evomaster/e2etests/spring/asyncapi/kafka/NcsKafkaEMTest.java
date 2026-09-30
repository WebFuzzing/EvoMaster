package org.evomaster.e2etests.spring.asyncapi.kafka;

import com.foo.asyncapi.ncs.NcsKafkaController;
import org.evomaster.core.problem.asyncapi.data.AsyncApiIndividual;
import org.evomaster.core.problem.asyncapi.data.AsyncApiOutcome;
import org.evomaster.core.problem.enterprise.ExperimentalFaultCategory;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.utils.AsyncApiTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A search over NCS driven through Kafka: every operation answers, and where the contract
 * declares a result and an error, the search reaches both.
 */
public class NcsKafkaEMTest extends AsyncApiTestBase {

    private static final List<String> OPERATIONS =
            Arrays.asList("checkTriangle", "bessj", "expint", "fisher", "gammq", "remainder");

    @BeforeAll
    public static void initClass() throws Exception {
        AsyncApiTestBase.initClass(new NcsKafkaController());
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
                        The broker is local and the service answers in milliseconds, so a reply
                        that has not arrived in a second is not coming. Left at its default, one
                        stalled message would eat a large share of the budget for this test.
                     */
                    args.add("--asyncApiReplyTimeoutMs");
                    args.add("1000");

                    /*
                        The kill switch stops SUT code that is still running once an individual
                        has been evaluated. It assumes a request handled on its own thread: here
                        the service consumes on one long-lived thread, so killing it stops the
                        service for good, and every later message goes unanswered. Any
                        message-driven SUT has this shape.
                     */
                    args.add("--killSwitch");
                    args.add("false");

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
                        service rejects within what it allows. These three describe their
                        constraint in prose rather than as a bound -- expint's x, gammq's a, and
                        fisher's x, which its degrees of freedom do not cover -- so the search is
                        free to produce it.

                        bessj and remainder declare an error reply that nothing obeying the
                        contract can provoke: their bounds are exactly the range the service
                        checks. That is a property of the document, not a gap in the search.
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

                    //the oracles are off here, so a well-behaved service must report nothing
                    assertTrue(faultsOf(solution).isEmpty(), "faults were reported: " + faultsOf(solution));
                },
                5);
    }

    @Test
    public void testRunEMInJava() throws Throwable {

        /*
            The lines that publish are written in the language the suite is generated in, and
            only one of the two is exercised by the test above. Compiling and running the other
            is what catches a Java half that was never compiled: the Kotlin one did not compile
            first time either.

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
                    setOption(args, "asyncApiReplyTimeoutMs", "1000");
                    setOption(args, "killSwitch", "false");

                    Solution<AsyncApiIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);
                    assertTrue(countOutcome(solution, AsyncApiOutcome.REPLIED) > 0, "nothing was answered");
                },
                5);
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

                    //as in testRunEM: the kill switch would stop the consumer thread for good
                    args.add("--killSwitch");
                    args.add("false");

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
