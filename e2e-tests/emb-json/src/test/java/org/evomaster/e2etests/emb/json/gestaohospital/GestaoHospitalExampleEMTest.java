package org.evomaster.e2etests.emb.json.gestaohospital;

import com.foo.rest.emb.json.gestaohospital.GestaoHospitalController;
import org.evomaster.core.EMConfig;
import org.evomaster.core.problem.rest.HttpVerb;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.emb.json.EMBJsonTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;

public class GestaoHospitalExampleEMTest extends EMBJsonTestBase {

    @BeforeAll
    public static void initClass() throws Exception {
        GestaoHospitalController controller = new GestaoHospitalController();
        EMConfig config = new EMConfig();
        config.getInstrumentMR_EXT_0();
        EMBJsonTestBase.initClass(controller, config);
    }

    @Disabled
    public void runEMTest() throws Throwable {
        runTestHandlingFlakyAndCompilation(
                "GestaoHospitalExampleEMTest",
                "org.foo.GestaoHospitalExampleEMTest",
                500,
                true,
                (args) -> {
                    Solution<RestIndividual> solution = initAndRun(args);

                    assertHasAtLeastOne(solution, HttpVerb.POST, 418, "/api/json", "Tea");
                },
                3
        );
    }
}
