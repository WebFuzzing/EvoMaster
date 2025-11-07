package org.evomaster.e2etests.spring.rpc.examples.authsetup;

import com.foo.rpc.examples.spring.authsetup.AuthLocalSetupController;
import com.foo.rpc.examples.spring.authsetup.AuthSetupService;
import org.evomaster.core.problem.rpc.RPCIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.rpc.examples.SpringRPCTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class AuthLocalSetupEMTest extends SpringRPCTestBase {


    @BeforeAll
    public static void initClass() throws Exception {
        SpringRPCTestBase.initClass(new AuthLocalSetupController());
    }

    @Test
    public void testRunEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "AuthLocalSetupEM",
                "org.bar.AuthLocalSetupEM",
                10,
                (args) -> {

                    Solution<RPCIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);
                    assertAllContentInResponseForEndpoint(solution, AuthSetupService.Iface.class.getName()+":access",
                            Arrays.asList("HELLO", "SORRY"));

                });
    }
}