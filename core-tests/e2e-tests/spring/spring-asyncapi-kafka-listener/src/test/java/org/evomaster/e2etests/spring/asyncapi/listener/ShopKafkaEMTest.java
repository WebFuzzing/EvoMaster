package org.evomaster.e2etests.spring.asyncapi.listener;

import com.foo.asyncapi.ShopKafkaController;
import com.google.inject.Injector;
import kotlin.Pair;
import org.evomaster.client.java.controller.api.dto.problem.asyncapi.AsyncApiReplyDto;
import org.evomaster.client.java.instrumentation.shared.ObjectiveNaming;
import org.evomaster.core.problem.asyncapi.data.AsyncApiIndividual;
import org.evomaster.core.problem.asyncapi.data.AsyncApiOutcome;
import org.evomaster.core.search.EvaluatedIndividual;
import org.evomaster.core.search.FitnessValue;
import org.evomaster.core.search.Heuristics;
import org.evomaster.core.search.Solution;
import org.evomaster.core.search.action.EvaluatedAction;
import org.evomaster.core.search.service.IdMapper;
import org.evomaster.e2etests.utils.AsyncApiTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A white-box search over a service that never answers: every operation is fire-and-forget, so
 * what the service did with a message is only known from its coverage, and that is what these
 * tests assert on.
 */
public class ShopKafkaEMTest extends AsyncApiTestBase {

    private static final String REQUEST_INVOICE = "requestInvoice";

    private static final String ADJUST_STOCK = "adjustStock";

    private static final String SUT_PACKAGE = "com.foo.asyncapi.shop.";

    private static final String EXPRESS_DISPATCH = SUT_PACKAGE + "ExpressDispatch";

    private static final String URGENT_ESCALATION = SUT_PACKAGE + "UrgentEscalation";

    private static final String INVOICE_RENDERER = SUT_PACKAGE + "InvoiceRenderer";

    private static final String OVERSOLD_STOCK = SUT_PACKAGE + "OversoldStock";

    private static final String DAMAGED_RETURN = SUT_PACKAGE + "DamagedReturn";

    @BeforeAll
    public static void initClass() throws Exception {
        AsyncApiTestBase.initClass(new ShopKafkaController());
    }

    /**
     * That some test in the solution credits the coverage of {@code className} to the action on
     * {@code operation} that caused it. The core records, for each target, the index of the
     * action open when the SUT first reached it in that test.
     *
     * {@code causesBefore} is how many actions on {@code operation} it takes before one can lead
     * to the class; a credit counts only when the test has no more than that before it, so none
     * of them could have landed late. Only the same test matters: the driver waits between tests.
     *
     * Some test, not every one: on a slow enough machine the work outlasts the window.
     */
    private void assertClassCreditedTo(
            Pair<Injector, Solution<AsyncApiIndividual>> run, String className, String operation, int causesBefore) {

        IdMapper idMapper = run.getFirst().getInstance(IdMapper.class);
        String target = ObjectiveNaming.classObjectiveName(className);

        //the operations of each test that covered the class, with the one credited in brackets
        List<String> credits = new ArrayList<>();
        boolean credited = false;
        for (EvaluatedIndividual<AsyncApiIndividual> test : run.getSecond().getIndividuals()) {
            List<String> operations = new ArrayList<>();
            for (EvaluatedAction action : test.evaluatedMainActions()) {
                operations.add(action.getAction().getName());
            }
            for (Map.Entry<Integer, Heuristics> reached : test.getFitness().getViewOfData().entrySet()) {
                if (!FitnessValue.Companion.isMaxValue(reached.getValue().getScore())
                        || !target.equals(idMapper.getDescriptiveId(reached.getKey()))) {
                    continue;
                }
                int index = reached.getValue().getActionIndex();
                if (index < 0 || index >= operations.size()) {
                    credits.add(operations + " [none: index " + index + "]");
                    continue;
                }
                credits.add(operations + " [" + index + "]");
                long earlier = operations.subList(0, index).stream().filter(operation::equals).count();
                if (operation.equals(operations.get(index)) && earlier <= causesBefore) {
                    credited = true;
                }
            }
        }

        assertTrue(credited, "With seed " + defaultSeed + ": " + className + " was never credited to the "
                + operation + " that caused it: " + credits);
    }

    /**
     * Express shipping and an urgent priority are both behind a string the document leaves free,
     * compared with {@code equals} for the one and read with {@code Priority.valueOf} for the
     * other. The search gets there by taint analysis, which is on by default in white-box mode:
     * the payload carries a tainted string, the service reports what it was compared with, and
     * the next mutation uses that value.
     */
    @Test
    public void testRunEM() throws Throwable {

        String outputFolder = "ShopKafkaEM";
        String className = "org.foo.asyncapi.ShopKafkaEM";

        /*
            The generated suite is compiled and run too. Its tests publish and move on, as the
            service gives them nothing to wait for.
         */
        runTestHandlingFlakyAndCompilation(
                outputFolder,
                className,
                null,
                300,
                true,
                false,
                (args) -> {

                    /*
                        Without a window, a test's results are collected as soon as its last
                        message is on the broker, often before the service read it, and what it
                        compared that message's strings with is lost. A handler here is done in
                        milliseconds, so most waits end on stable coverage.
                     */
                    setOption(args, "asyncApiSettleMs", "2000");
                    setOption(args, "asyncApiCompletion", "COVERAGE_STABLE");
                    setOption(args, "asyncApiStableMs", "100");

                    //more, shorter tests: each is another chance to use a value the service compared with
                    setOption(args, "maxTestSize", "3");

                    Pair<Injector, Solution<AsyncApiIndividual>> run = initAndDebug(args);
                    Solution<AsyncApiIndividual> solution = run.getSecond();

                    assertTrue(countOutcome(solution, AsyncApiOutcome.PUBLISHED) > 0, "nothing was published");
                    assertEquals(0, countOutcome(solution, AsyncApiOutcome.PUBLISH_FAILED), "a message never left");

                    //stable coverage is what let messages go, not only the deadline
                    assertTrue(countSettleEnd(solution, AsyncApiReplyDto.SettleEnd.COVERAGE_STABLE) > 0,
                            "With seed " + defaultSeed + ": no wait ended on stable coverage");

                    assertClassCovered(run, EXPRESS_DISPATCH);
                    assertClassCovered(run, URGENT_ESCALATION);
                },
                5);

        //the tests that reach those classes are kept, with the values that got there
        assertTextInTests(outputFolder, className, "EXPRESS");
        assertTextInTests(outputFolder, className, "URGENT");
    }

    /**
     * Two things a message leads to that the handler returning says nothing about: an invoice
     * rendered on another thread a while after the listener has returned, and a product sold
     * beyond its stock, which needs an earlier message of the same test to have been handled
     * first. With a fixed window longer than both take, they are covered, and credited to the
     * messages that caused them.
     */
    @Test
    public void testRunEMWithASettleWindow() throws Throwable {

        runTestHandlingFlaky(
                "ShopKafkaSettleEM",
                "org.foo.asyncapi.ShopKafkaSettleEM",
                150,
                false,
                (args) -> {

                    /*
                        Rendering an invoice takes 150 ms after the listener returns. A window
                        more than three times that leaves room for a loaded machine, while each
                        test still costs little.
                     */
                    setOption(args, "asyncApiSettleMs", "500");

                    Pair<Injector, Solution<AsyncApiIndividual>> run = initAndDebug(args);
                    Solution<AsyncApiIndividual> solution = run.getSecond();

                    assertEquals(0, countOutcome(solution, AsyncApiOutcome.PUBLISH_FAILED), "a message never left");
                    assertEverySettled(solution);

                    assertClassCovered(run, INVOICE_RENDERER);
                    assertClassCovered(run, OVERSOLD_STOCK);

                    /*
                        Covered at all could be late coverage counted on whatever action was open.
                        An invoice is rendered for any request; overselling needs one earlier
                        adjustment.
                     */
                    assertClassCreditedTo(run, INVOICE_RENDERER, REQUEST_INVOICE, 0);
                    assertClassCreditedTo(run, OVERSOLD_STOCK, ADJUST_STOCK, 1);
                },
                5);
    }

    /**
     * The document declares no payload for a return, so without expanding opaque payloads the
     * operation is not even published. With it, the return is published as an empty object, the
     * service reads it into {@code ReturnRequested}, and the next mutation publishes that class's
     * fields instead. A refund is behind its free-text reason, which taint analysis then finds.
     */
    @Test
    public void testRunEMExpandingOpaquePayloads() throws Throwable {

        String outputFolder = "ShopKafkaOpaqueEM";
        String className = "org.foo.asyncapi.ShopKafkaOpaqueEM";

        //compiled and run too: the payload the generated tests publish is the expanded one
        runTestHandlingFlakyAndCompilation(
                outputFolder,
                className,
                null,
                400,
                true,
                false,
                (args) -> {

                    setOption(args, "expandAsyncApiPayloads", "true");

                    /*
                        The class a message is read into is credited to the action open when the
                        service read it, so a stable-coverage window puts it on the right one. Short
                        tests, as in testRunEM.
                     */
                    setOption(args, "asyncApiSettleMs", "2000");
                    setOption(args, "asyncApiCompletion", "COVERAGE_STABLE");
                    setOption(args, "asyncApiStableMs", "100");
                    setOption(args, "maxTestSize", "3");

                    Pair<Injector, Solution<AsyncApiIndividual>> run = initAndDebug(args);
                    Solution<AsyncApiIndividual> solution = run.getSecond();

                    assertEquals(0, countOutcome(solution, AsyncApiOutcome.PUBLISH_FAILED), "a message never left");

                    assertClassCovered(run, DAMAGED_RETURN);
                },
                5);

        //the field the document never names, with the value that got the refund
        assertTextInTests(outputFolder, className, "reason");
        assertTextInTests(outputFolder, className, "DAMAGED");
    }
}
