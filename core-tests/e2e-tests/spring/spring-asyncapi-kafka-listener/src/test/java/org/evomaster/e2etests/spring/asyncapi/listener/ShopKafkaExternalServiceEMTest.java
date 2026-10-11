package org.evomaster.e2etests.spring.asyncapi.listener;

import com.foo.asyncapi.ShopKafkaController;
import com.google.inject.Injector;
import kotlin.Pair;
import org.evomaster.core.EMConfig;
import org.evomaster.core.problem.asyncapi.data.AsyncApiIndividual;
import org.evomaster.core.problem.asyncapi.data.AsyncApiOutcome;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.utils.AsyncApiTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.ServerSocket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * A white-box search over a message handler that calls an HTTP service, mocked by EvoMaster with
 * WireMock as it is for REST.
 *
 * The restock handler asks a supplier at a hostname that does not resolve. Unmocked, every call
 * fails and neither branch on the answer is reached; mocked, the hostname resolves to a local
 * WireMock and the search chooses the answer.
 *
 * WireMock is bound to an address in 127.0.0.0/8 other than 127.0.0.1. That is a loopback address
 * on Linux, where CI runs, but not on macOS unless an alias is added (see docs/loopback.md). The
 * test is skipped where nothing can bind there, rather than failed.
 */
public class ShopKafkaExternalServiceEMTest extends AsyncApiTestBase {

    private static final String SUT_PACKAGE = "com.foo.asyncapi.shop.";

    private static final String RESTOCK_CONFIRMED = SUT_PACKAGE + "RestockConfirmed";

    private static final String RESTOCK_REFUSED = SUT_PACKAGE + "RestockRefused";

    /**
     * Where the WireMock instances bind. Not one of the reserved 127.0.0.0 to 127.0.0.3.
     */
    private static final String MOCK_IP = "127.0.0.92";

    private static final String SUPPLIER_HOST = "supplier.test";

    @BeforeAll
    public static void initClass() throws Exception {

        assumeTrue(canBind(MOCK_IP), "nothing can bind to " + MOCK_IP + " here, see docs/loopback.md");

        /*
            The NET replacements see which hostnames the service resolves and connects to, and
            redirect them to WireMock. Off by default, and applied as classes load, so set before
            the service starts.
         */
        EMConfig config = new EMConfig();
        config.setInstrumentMR_NET(true);
        initClass(new ShopKafkaController(ShopKafkaController.RESTOCK_DOCUMENT), config);
    }

    private static boolean canBind(String address) {
        try (ServerSocket socket = new ServerSocket(0, 0, InetAddress.getByName(address))) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * That the supplier answering something other than 200 is reached shows the call was
     * redirected to WireMock, which answers with a default response. That 200 is reached shows
     * the search chose the answer: nothing else returns it.
     */
    @Test
    public void testRunEM() throws Throwable {

        String outputFolder = "ShopKafkaExternalServiceEM";
        String className = "org.foo.asyncapi.ShopKafkaExternalServiceEM";

        /*
            The generated suite is compiled and run too: it starts WireMock where the search did,
            points the supplier's hostname at it, and stubs what the search chose before each
            message is published.
         */
        runTestHandlingFlakyAndCompilation(
                outputFolder,
                className,
                null,
                150,
                true,
                false,
                (args) -> {

                    setOption(args, "externalServiceIPSelectionStrategy", "USER");
                    setOption(args, "externalServiceIP", MOCK_IP);

                    /*
                        The handler calls the supplier after the driver has returned, and what
                        WireMock served is read when the controller returns: without a window the
                        call is missed. Half a second is far more than a handler here takes.
                     */
                    setOption(args, "asyncApiSettleMs", "500");

                    //one message per test: what matters is what the supplier answers to it
                    setOption(args, "maxTestSize", "1");

                    Pair<Injector, Solution<AsyncApiIndividual>> run = initAndDebug(args);
                    Solution<AsyncApiIndividual> solution = run.getSecond();

                    assertEquals(0, countOutcome(solution, AsyncApiOutcome.PUBLISH_FAILED), "a message never left");

                    assertClassCovered(run, RESTOCK_REFUSED);
                    assertClassCovered(run, RESTOCK_CONFIRMED);
                },
                5);

        assertTextInTests(outputFolder, className, "DnsCacheManipulator.setDnsCache(\"" + SUPPLIER_HOST + "\"");
        assertTextInTests(outputFolder, className, ".withStatus(200)");
    }
}
