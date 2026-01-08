package org.evomaster.e2etests.emb.json.genome;

import com.foo.rest.emb.json.genome.GenomeNexusExampleController;
import org.evomaster.core.EMConfig;
import org.evomaster.core.problem.rest.data.HttpVerb;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.emb.json.EMBJsonTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class GenomeNexusExampleEMTest extends EMBJsonTestBase {

    @BeforeAll
    public static void initClass() throws Exception {
        GenomeNexusExampleController controller = new GenomeNexusExampleController();
        EMConfig config = new EMConfig();
        config.getInstrumentMR_EXT_0();
        EMBJsonTestBase.initClass(controller, config);
    }

    @Test
    public void runEMTest() throws Throwable {
        runTestHandlingFlakyAndCompilation(
                "GenomeNexusExampleGeneratedEMTest",
                "org.bar.GenomeNexusExampleGeneratedEMTest",
                500,
                true,
                (args) -> {
                    Solution<RestIndividual> solution = initAndRun(args);

                    assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/json", "Teal");
                },
                3
        );
    }
}
