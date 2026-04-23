package org.evomaster.e2etests.spring.rpc.examples.authsetup;

import com.foo.rpc.examples.spring.authsetup.AuthSetupController;
import com.foo.rpc.examples.spring.authsetup.AuthSetupService;
import org.evomaster.core.problem.rpc.RPCIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.rpc.examples.SpringRPCTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class AuthSetupEMTest extends SpringRPCTestBase {


    @BeforeAll
    public static void initClass() throws Exception {
        SpringRPCTestBase.initClass(new AuthSetupController());
    }

    @Test
    public void testRunEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "AuthSetupEM",
                "org.bar.AuthSetupEM",
                10,
                (args) -> {

                    Solution<RPCIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);
                    assertAllContentInResponseForEndpoint(solution, AuthSetupService.Iface.class.getName()+":access",
                            Arrays.asList("HELLO", "SORRY"));

                });
    }
}