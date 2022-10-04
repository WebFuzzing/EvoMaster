package org.evomaster.e2etests.spring.rpc.examples.testability;

import com.foo.rpc.examples.spring.testability.TestabilityController;
import com.foo.rpc.examples.spring.testability.TestabilityService;
import com.foo.rpc.examples.spring.testability.TestabilityWithSeedTestController;
import org.evomaster.core.problem.rpc.RPCCallResultCategory;
import org.evomaster.core.problem.rpc.RPCIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.rpc.examples.SpringRPCTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestabilityWithSeedTestEMTest extends SpringRPCTestBase {


    @BeforeAll
    public static void initClass() throws Exception {
        SpringRPCTestBase.initClass(new TestabilityWithSeedTestController());
    }


    @Test
    public void testRunEM() throws Throwable {


        runTestHandlingFlakyAndCompilation(
                "TestabilityWithSeedTestEM",
                "org.bar.TestabilityWithSeedTestEM",
                10,
                (args) -> {
                    args.add("--baseTaintAnalysisProbability");
                    args.add("0.9");
                    args.add("--seedTestCases");
                    args.add("true");

                    Solution<RPCIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    assertRPCEndpointResult(solution, TestabilityService.Iface.class.getName()+":getSeparated", RPCCallResultCategory.HANDLED.name());
                    assertAllContentInResponseForEndpoint(solution,TestabilityService.Iface.class.getName()+":getSeparated" , Arrays.asList("ERROR", "OK"));
                });
    }
}

