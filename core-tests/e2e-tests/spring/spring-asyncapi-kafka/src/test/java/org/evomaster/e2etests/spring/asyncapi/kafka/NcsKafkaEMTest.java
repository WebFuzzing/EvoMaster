package org.evomaster.e2etests.spring.asyncapi.kafka;

import com.foo.asyncapi.ncs.NcsKafkaController;
import org.evomaster.core.problem.asyncapi.data.AsyncApiIndividual;
import org.evomaster.core.problem.asyncapi.data.AsyncApiOutcome;
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

        //no test writer for AsyncAPI yet, so the search alone
        runTestHandlingFlaky(
                "NcsKafkaEM",
                "org.foo.asyncapi.NcsKafkaEM",
                300,
                false,
                (args) -> {

                    Solution<AsyncApiIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    for (String operation : OPERATIONS) {
                        assertReplied(solution, operation);
                    }

                    assertReplyReached(solution, "checkTriangle", "intResult");
                    assertReplyReached(solution, "bessj", "doubleResult");
                    assertReplyReached(solution, "remainder", "intResult");

                    /*
                        Of the rejections NCS makes, only these two lie within what the schema
                        allows: expint rejects a negative x, gammq a non-positive a or a negative
                        x. bessj's order and remainder's operands are bounded by the schema, so
                        their error replies cannot be reached without publishing invalid data.
                     */
                    assertReplyReached(solution, "expint", "doubleResult");
                    assertReplyReached(solution, "expint", "error");
                    assertReplyReached(solution, "gammq", "doubleResult");
                    assertReplyReached(solution, "gammq", "error");

                    assertEquals(0, countOutcome(solution, AsyncApiOutcome.NO_REPLY), "a promised reply never came");
                    assertEquals(0, countOutcome(solution, AsyncApiOutcome.PUBLISH_FAILED), "a message never left");
                },
                5);
    }
}
