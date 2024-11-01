package org.evomaster.e2etests.emb.json.language;

import com.foo.rest.emb.json.language.LanguageServerExampleController;
import org.evomaster.core.EMConfig;
import org.evomaster.core.problem.rest.HttpVerb;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.emb.json.EMBJsonTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class LanguageServerExampleEMTest extends EMBJsonTestBase {

    @BeforeAll
    public static void initClass() throws Exception {
        LanguageServerExampleController controller = new LanguageServerExampleController();
        EMConfig config = new EMConfig();
        config.getInstrumentMR_EXT_0();
        EMBJsonTestBase.initClass(controller, config);
    }

    @Disabled
    @Test
    public void runEMTest() throws Throwable {
        // Similar to Gestao example, if the map value is a object
        // we are not solving it now.
        // {
        //                    "EM_tainted_map":"_EM_28_XYZ_"
        //                    ,
        //                    "matches":""
        //                    }
        runTestHandlingFlakyAndCompilation(
                "LanguageServerExampleEMTest",
                "org.foo.LanguageServerExampleEMTest",
                500,
                true,
                (args) -> {
                    Solution<RestIndividual> solution = initAndRun(args);

                    assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/json", "vowels");
                },
                3
        );
    }
}
