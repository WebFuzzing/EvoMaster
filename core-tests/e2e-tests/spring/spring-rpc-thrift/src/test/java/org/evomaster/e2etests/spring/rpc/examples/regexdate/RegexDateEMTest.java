package org.evomaster.e2etests.spring.rpc.examples.regexdate;

import com.foo.rpc.examples.spring.regexdate.RegexDateController;
import com.foo.rpc.examples.spring.regexdate.RegexDateService;
import org.evomaster.core.problem.rpc.RPCCallResultCategory;
import org.evomaster.core.problem.rpc.RPCIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.rpc.examples.SpringRPCTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class RegexDateEMTest extends SpringRPCTestBase {


    @BeforeAll
    public static void initClass() throws Exception {
        SpringRPCTestBase.initClass(new RegexDateController());
    }


    @Test
    public void testRunEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "RegexDateEM",
                "org.bar.RegexDateEM",
                1000,
                (args) -> {

                    args.add("--baseTaintAnalysisProbability");
                    args.add("0.9");

                    Solution<RPCIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);
                    assertRPCEndpointResult(solution, RegexDateService.Iface.class.getName()+":get", RPCCallResultCategory.HANDLED.name());
                    assertContentInResponseForEndpoint(solution, RegexDateService.Iface.class.getName()+":get", "OK");
                    assertResponseContainException(solution, "APP_INTERNAL_ERROR");
                });
    }
}
