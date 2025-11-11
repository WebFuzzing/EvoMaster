package org.evomaster.e2etests.emb.json.proxyprint;

import com.foo.rest.emb.json.proxyprint.ProxyPrintExampleController;
import org.evomaster.core.EMConfig;
import org.evomaster.core.problem.rest.data.HttpVerb;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.emb.json.EMBJsonTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ProxyPrintExampleEMTest extends EMBJsonTestBase {

    @BeforeAll
    public static void initClass() throws Exception {
        ProxyPrintExampleController controller = new ProxyPrintExampleController();
        EMConfig config = new EMConfig();
        config.getInstrumentMR_EXT_0();
        EMBJsonTestBase.initClass(controller, config);
    }

    @Test
    public void runEMTest() throws Throwable {
        runTestHandlingFlakyAndCompilation(
                "ProxyPrintExampleGeneratedEMTest",
                "org.foo.ProxyPrintExampleGeneratedEMTest",
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
