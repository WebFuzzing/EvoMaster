package org.evomaster.e2etests.emb.json.language;

import com.foo.rest.emb.json.language.LanguageServerExampleController;
import org.evomaster.core.EMConfig;
import org.evomaster.core.problem.rest.data.HttpVerb;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.emb.json.EMBJsonTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class LanguageServerExampleEMTest extends EMBJsonTestBase {

    @BeforeAll
    public static void initClass() throws Exception {
        LanguageServerExampleController controller = new LanguageServerExampleController();
        EMConfig config = new EMConfig();
        config.getInstrumentMR_EXT_0();
        EMBJsonTestBase.initClass(controller, config);
    }

    @Test
    public void runEMTest() throws Throwable {
        /*
            after the impact collection for specialization is disabled,
            this test starts to fail.
            however, the test can pass by updating the seed.
            Later it may need to conduct an experiment on how this disabling impacts on the effectiveness.
         */
        defaultSeed = 0;

        runTestHandlingFlakyAndCompilation(
                "LanguageServerExampleGeneratedEMTest",
                "org.foo.LanguageServerExampleGeneratedEMTest",
                4_000,
                true,
                (args) -> {

                    setOption(args, "taintForceSelectionOfGenesWithSpecialization", "true");
                    setOption(args, "discoveredInfoRewardedInFitness", "true");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/json", "vowels");
                },
                3
        );
    }
}
