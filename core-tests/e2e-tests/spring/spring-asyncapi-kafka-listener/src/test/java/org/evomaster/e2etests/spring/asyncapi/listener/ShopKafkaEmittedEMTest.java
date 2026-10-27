package org.evomaster.e2etests.spring.asyncapi.listener;

import com.foo.asyncapi.ShopKafkaController;
import com.foo.asyncapi.shop.ShopTopics;
import com.google.inject.Injector;
import kotlin.Pair;
import org.evomaster.core.EMConfig;
import org.evomaster.core.problem.asyncapi.data.AsyncApiIndividual;
import org.evomaster.core.problem.asyncapi.data.AsyncApiOutcome;
import org.evomaster.core.problem.enterprise.ExperimentalFaultCategory;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.utils.AsyncApiTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What the shop service publishes while handling a message, as the Kafka instrumentation records
 * it: a confirmation for every order, to a topic the document declares for it to send to, and a
 * change of stock for every adjustment, to one the document does not declare.
 *
 * A class of its own, as {@link ShopKafkaHandlerReturnEMTest} is, for the same reason: the Kafka
 * replacements can only be asked for when {@link #initClass()} starts the service.
 */
public class ShopKafkaEmittedEMTest extends AsyncApiTestBase {

    private static final String KOTLIN = ".kt";

    private static final String PLACE_ORDER = "placeOrder";

    private static final String ADJUST_STOCK = "adjustStock";

    private static final String EMITTED_TARGET = "ASYNCAPI_EMITTED:" + ShopTopics.ORDERS_CONFIRMED + ":" + PLACE_ORDER;

    private static final String EMITTED_MESSAGE_TARGET = "ASYNCAPI_EMITTED_MESSAGE:orderConfirmed:" + PLACE_ORDER;

    @BeforeAll
    public static void initClass() throws Exception {

        EMConfig config = new EMConfig();
        config.setInstrumentMR_KAFKA(true);

        AsyncApiTestBase.initClass(new ShopKafkaController(), config);
    }

    /**
     * The confirmation is a target, and the change of stock a fault, once experimental oracles
     * are asked for. The generated suite is compiled and run too, and its tests wait for the
     * confirmation and assert on it, rather than publishing and moving on.
     */
    @Test
    public void testRunEM() throws Throwable {

        String outputFolder = "ShopKafkaEmittedEM";
        String className = "org.foo.asyncapi.ShopKafkaEmittedEM";

        runTestHandlingFlakyAndCompilation(outputFolder, className, null, 60, true, false,
                args -> runAndCheck(args), 5);

        assertGeneratedTestsObserve(outputFolder, className, KOTLIN);
    }

    private void runAndCheck(List<String> args) {

        /*
            The listener publishes the confirmation after the driver has returned: a window records
            it on the action that caused it, and ends on stable coverage, which here takes
            milliseconds.
         */
        setOption(args, "asyncApiSettleMs", "2000");
        setOption(args, "asyncApiCompletion", "COVERAGE_STABLE");
        setOption(args, "asyncApiStableMs", "100");
        setOption(args, "instrumentMR_KAFKA", "true");

        //an emission to an undeclared address is an experimental fault
        setOption(args, "useExperimentalOracles", "true");

        Pair<Injector, Solution<AsyncApiIndividual>> run = initAndDebug(args);
        Solution<AsyncApiIndividual> solution = run.getSecond();

        assertEquals(0, countOutcome(solution, AsyncApiOutcome.PUBLISH_FAILED), "a message never left");

        assertTargetCovered(run, EMITTED_TARGET);
        assertTargetCovered(run, EMITTED_MESSAGE_TARGET);

        assertTrue(faultsOf(solution).contains(ExperimentalFaultCategory.ASYNCAPI_UNDECLARED_EMISSION),
                "With seed " + defaultSeed + ": no undeclared emission was reported, only " + faultsOf(solution));

        //the stock topic is undeclared, so it is never what a test observes
        boolean stockObserved = resultsOf(solution, ADJUST_STOCK).stream()
                .anyMatch(r -> r.getDeclaredEmissionAddresses().contains(ShopTopics.STOCK_CHANGES));
        assertTrue(!stockObserved, "the undeclared stock topic was taken for a declared one");
    }

    /**
     * That the suite written to {@code outputFolder} observes the confirmation topic around a
     * publish, rather than leaving it to compiling and running to pass with nothing observed.
     */
    private void assertGeneratedTestsObserve(String outputFolder, String className, String extension) throws IOException {
        Path suite = Paths.get(outputFolderPath(outputFolder), className.replace('.', '/') + extension);
        String text = new String(Files.readAllBytes(suite), StandardCharsets.UTF_8);
        for (String expected : Arrays.asList("observeEmitted(", "awaitEmitted(", "\"" + ShopTopics.ORDERS_CONFIRMED + "\"")) {
            assertTrue(text.contains(expected), "No " + expected + " in " + suite);
        }
    }
}
