package org.evomaster.e2etests.spring.rpc.examples.numericstring;


import com.foo.rpc.examples.spring.numericstring.NumericStringService;
import org.evomaster.core.problem.rpc.RPCCallResultCategory;
import org.evomaster.core.problem.rpc.RPCIndividual;
import org.evomaster.core.search.Solution;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class NumericStringEMTest extends NumericStringTestBase {

    @Test
    public void testRunEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "NumericStringEM",
                "org.bar.NumericStringEM",
                20000,
                (args) -> {


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
