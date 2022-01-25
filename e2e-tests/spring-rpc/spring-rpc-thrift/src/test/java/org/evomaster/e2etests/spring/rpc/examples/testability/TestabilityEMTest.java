package org.evomaster.e2etests.spring.rpc.examples.testability;

import com.foo.rpc.examples.spring.testability.TestabilityController;
import com.foo.rpc.examples.spring.testability.TestabilityService;
import org.evomaster.ci.utils.CIUtils;
import org.evomaster.core.problem.rpc.RPCCallResultCategory;
import org.evomaster.core.problem.rpc.RPCIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.rpc.examples.SpringRPCTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestabilityEMTest extends SpringRPCTestBase {


    @BeforeAll
    public static void initClass() throws Exception {
        SpringRPCTestBase.initClass(new TestabilityController());
    }


    @Test
    public void testRunEM() throws Throwable {

        //TODO check it later, only fail on CI
        CIUtils.skipIfOnGA();

        runTestHandlingFlakyAndCompilation(
                "TestabilityEM",
                "org.bar.TestabilityEM",
                15_000,
                (args) -> {
                    args.add("--baseTaintAnalysisProbability");
                    args.add("0.9");

                    Solution<RPCIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    assertRPCEndpointResult(solution, TestabilityService.Iface.class.getName()+":getSeparated", RPCCallResultCategory.HANDLED.name());
                    assertAllContentInResponseForEndpoint(solution,TestabilityService.Iface.class.getName()+":getSeparated" , Arrays.asList("ERROR", "OK"));
                });
    }
}

