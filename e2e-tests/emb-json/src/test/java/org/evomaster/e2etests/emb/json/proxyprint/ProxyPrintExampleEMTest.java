package org.evomaster.e2etests.emb.json.proxyprint;

import com.foo.rest.emb.json.proxyprint.ProxyPrintExampleController;
import org.evomaster.core.EMConfig;
import org.evomaster.core.problem.rest.HttpVerb;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.emb.json.EMBJsonTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class ProxyPrintExampleEMTest extends EMBJsonTestBase {

    @BeforeAll
    public static void initClass() throws Exception {
        ProxyPrintExampleController controller = new ProxyPrintExampleController();
        EMConfig config = new EMConfig();
        config.getInstrumentMR_EXT_0();
        EMBJsonTestBase.initClass(controller, config);
    }

    @Disabled
    @Test
    public void runEMTest() throws Throwable {
        // In PrintRequestController, we managed to reach line 25.
        // Although when the code query for a list of "printshops" from the Map,
        // it fails.
        runTestHandlingFlakyAndCompilation(
                "ProxyPrintExampleEMTest",
                "org.foo.ProxyPrintExampleEMTest",
                5_000,
                true,
                (args) -> {
                    setOption(args, "taintForceSelectionOfGenesWithSpecialization", "true");
                    setOption(args, "discoveredInfoRewardedInFitness", "true");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/json", "Printing");
                },
                3
        );
    }
}
