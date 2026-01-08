package org.evomaster.e2etests.emb.json.tiltaksgjennomforing;

import com.foo.rest.emb.json.tiltaksgjennomforing.TiltaksgjennomforingExampleController;
import org.evomaster.core.EMConfig;
import org.evomaster.core.problem.rest.data.HttpVerb;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.emb.json.EMBJsonTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TiltaksgjennomforingExampleEMTest extends EMBJsonTestBase {

    @BeforeAll
    public static void initClass() throws Exception {
        TiltaksgjennomforingExampleController controller = new TiltaksgjennomforingExampleController();
        EMConfig config = new EMConfig();
        config.getInstrumentMR_EXT_0();
        EMBJsonTestBase.initClass(controller, config);
    }

    @Test
    public void runEMTest() throws Throwable {
        runTestHandlingFlakyAndCompilation(
                "TiltaksgjennomforingExampleGeneratedEMTest",
                "org.foo.TiltaksgjennomforingExampleGeneratedEMTest",
                5_000,
                true,
                (args) -> {

                    setOption(args, "taintForceSelectionOfGenesWithSpecialization", "true");
                    setOption(args, "discoveredInfoRewardedInFitness", "true");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/json", "Approved");
                },
                3
        );
    }
}
