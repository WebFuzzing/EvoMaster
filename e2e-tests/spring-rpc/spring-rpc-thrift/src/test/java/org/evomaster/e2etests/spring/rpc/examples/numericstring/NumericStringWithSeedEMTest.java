package org.evomaster.e2etests.spring.rpc.examples.numericstring;

import com.foo.rpc.examples.spring.numericstring.NumericStringService;
import com.foo.rpc.examples.spring.numericstring.NumericStringWithSeedTestController;
import org.evomaster.core.problem.rpc.RPCCallResultCategory;
import org.evomaster.core.problem.rpc.RPCIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.rpc.examples.SpringRPCTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class NumericStringWithSeedEMTest extends SpringRPCTestBase {

    @BeforeAll
    public static void initClass() throws Exception {
        SpringRPCTestBase.initClass(new NumericStringWithSeedTestController());
    }

    @Test
    public void testRunEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "NumericStringWithSeedEM",
                "org.bar.NumericStringWithSeedEM",
                60,
                (args) -> {
                    args.add("--seedTestCases");
                    args.add("true");


                    Solution<RPCIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);
                    assertRPCEndpointResult(solution, NumericStringService.Iface.class.getName()+":getNumber", RPCCallResultCategory.HANDLED.name());
                    assertAllContentInResponseForEndpoint(solution, NumericStringService.Iface.class.getName()+":getNumber",
                            Arrays.asList("NULL","LONG", "INT", "DOUBLE", "L_FOUND", "I_FOUND","D_FOUND","0_FOUND"));
                    // all values should conform to numeric format, ERROR should not exist in the response
                    assertNoneContentInResponseForEndpoint(solution, NumericStringService.Iface.class.getName()+":getNumber",
                            Arrays.asList("ERROR"));
                });
    }
}
