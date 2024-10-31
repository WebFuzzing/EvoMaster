package org.evomaster.e2etests.emb.json.ocvn;

import com.foo.rest.emb.json.ocvn.OcvnExampleController;
import org.evomaster.core.EMConfig;
import org.evomaster.core.problem.rest.HttpVerb;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.emb.json.EMBJsonTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class OcvnExampleEMTest extends EMBJsonTestBase {

    @BeforeAll
    public static void initClass() throws Exception {
        OcvnExampleController controller = new OcvnExampleController();
        EMConfig config = new EMConfig();
        config.getInstrumentMR_EXT_0();
        EMBJsonTestBase.initClass(controller, config);
    }

    @Test
    public void runEMTest() throws Throwable {
        runTestHandlingFlakyAndCompilation(
                "OcvnExampleEMTest",
                "org.foo.OcvnExampleEMTest",
                500,
                true,
                (args) -> {

                    setOption(args, "taintForceSelectionOfGenesWithSpecialization", "true");
                    setOption(args, "discoveredInfoRewardedInFitness", "true");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/json", "Darwin");
                },
                3
        );
    }
}
