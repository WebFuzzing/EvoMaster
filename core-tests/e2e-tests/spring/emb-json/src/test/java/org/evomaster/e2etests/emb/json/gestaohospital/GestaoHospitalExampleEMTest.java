package org.evomaster.e2etests.emb.json.gestaohospital;

import com.foo.rest.emb.json.gestaohospital.GestaoHospitalController;
import org.evomaster.core.EMConfig;
import org.evomaster.core.problem.rest.data.HttpVerb;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.emb.json.EMBJsonTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class GestaoHospitalExampleEMTest extends EMBJsonTestBase {

    @BeforeAll
    public static void initClass() throws Exception {
        GestaoHospitalController controller = new GestaoHospitalController();
        EMConfig config = new EMConfig();
        config.getInstrumentMR_EXT_0();
        EMBJsonTestBase.initClass(controller, config);
    }

    @Test
    public void runEMTest() throws Throwable {
        runTestHandlingFlakyAndCompilation(
                "GestaoHospitalExampleGeneratedEMTest",
                "org.foo.GestaoHospitalExampleGeneratedEMTest",
                5_000,
                true,
                (args) -> {
                    setOption(args, "taintForceSelectionOfGenesWithSpecialization", "true");
                    setOption(args, "discoveredInfoRewardedInFitness", "true");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertHasAtLeastOne(solution, HttpVerb.POST, 418, "/api/json", "Tea");
                },
                3
        );
    }
}
