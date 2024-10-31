package org.evomaster.e2etests.emb.json.tiltaksgjennomforing;

import com.foo.rest.emb.json.tiltaksgjennomforing.TiltaksgjennomforingExampleController;
import org.evomaster.core.EMConfig;
import org.evomaster.core.problem.rest.HttpVerb;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.emb.json.EMBJsonTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;

public class TiltaksgjennomforingExampleEMTest extends EMBJsonTestBase {

    @BeforeAll
    public static void initClass() throws Exception {
        TiltaksgjennomforingExampleController controller = new TiltaksgjennomforingExampleController();
        EMConfig config = new EMConfig();
        config.getInstrumentMR_EXT_0();
        EMBJsonTestBase.initClass(controller, config);
    }

    @Disabled
    public void runEMTest() throws Throwable {
        runTestHandlingFlakyAndCompilation(
                "TiltaksgjennomforingExampleEMTest",
                "org.foo.TiltaksgjennomforingExampleEMTest",
                500,
                true,
                (args) -> {
                    Solution<RestIndividual> solution = initAndRun(args);

                    assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/read", "Approved");
                    assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/convert", "Approved");
                },
                3
        );
    }
}
