package org.evomaster.e2etests.spring.rpc.examples.customization;

import com.foo.rpc.examples.spring.customization.CustomizationController;
import com.foo.rpc.examples.spring.customization.CustomizationService;
import org.evomaster.core.problem.rpc.RPCIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.rpc.examples.SpringRPCTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CustomizationEMTest extends SpringRPCTestBase {


    @BeforeAll
    public static void initClass() throws Exception {
        SpringRPCTestBase.initClass(new CustomizationController());
    }

    @Test
    public void testRunEM() throws Throwable {

        assertThrows(Throwable.class, ()-> runTestHandlingFlakyAndCompilation(
                "CustomizationEM",
                "org.bar.CustomizationEM",
                300,
                (args) -> {

                    Solution<RPCIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);
                    assertAllContentInResponseForEndpoint(solution, CustomizationService.Iface.class.getName()+":handleDependent",
                            Arrays.asList("0", "1","43","101"));
                    assertAllContentInResponseForEndpoint(solution, CustomizationService.Iface.class.getName()+":handleCombinedSeed",
                            Arrays.asList("-1","0", "1","43","101"));

                }));
    }
}
