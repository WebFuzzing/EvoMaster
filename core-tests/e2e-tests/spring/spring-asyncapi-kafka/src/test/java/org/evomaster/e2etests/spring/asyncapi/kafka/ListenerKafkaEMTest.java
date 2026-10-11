package org.evomaster.e2etests.spring.asyncapi.kafka;

import com.foo.asyncapi.kafka.listener.ExpressDispatch;
import com.foo.asyncapi.kafka.listener.ListenerKafkaController;
import com.foo.asyncapi.kafka.listener.OrderListener;
import com.google.inject.Injector;
import kotlin.Pair;
import org.evomaster.core.problem.asyncapi.data.AsyncApiIndividual;
import org.evomaster.core.problem.asyncapi.data.AsyncApiOutcome;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.utils.AsyncApiTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A search over a service that never answers, so what it did with a message is only known from
 * its coverage. An express order is behind a string the contract leaves free, which taint
 * analysis finds, and is dispatched on another thread after the listener returns, which only a
 * settle window waits for.
 */
public class ListenerKafkaEMTest extends AsyncApiTestBase {

    @BeforeAll
    public static void initClass() throws Exception {
        AsyncApiTestBase.initClass(new ListenerKafkaController());
    }

    @Test
    public void testRunEM() throws Throwable {

        String outputFolder = "ListenerKafkaEM";
        String className = "org.foo.asyncapi.ListenerKafkaEM";

        //the generated suite is compiled and run too, publishing and then sleeping the same window
        runTestHandlingFlakyAndCompilation(
                outputFolder,
                className,
                null,
                50,
                true,
                false,
                (args) -> {

                    //several times what the dispatch takes, for a loaded machine
                    setOption(args, "asyncApiSettleMs", String.valueOf(3 * OrderListener.DISPATCH_MS));
                    //one message per test: each test is another chance to use a value the service compared with
                    setOption(args, "maxTestSize", "1");

                    Pair<Injector, Solution<AsyncApiIndividual>> run = initAndDebug(args);
                    Solution<AsyncApiIndividual> solution = run.getSecond();

                    assertEquals(0, countOutcome(solution, AsyncApiOutcome.PUBLISH_FAILED), "a message never left");
                    assertClassCovered(run, ExpressDispatch.class.getName());
                },
                5);

        //the test that dispatches is kept, with the value that got there
        assertTextInTests(outputFolder, className, OrderListener.EXPRESS);
    }
}
