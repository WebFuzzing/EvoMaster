package org.evomaster.e2etests.emb.json.language;

import com.foo.rest.emb.json.language.LanguageServerExampleController;
import org.evomaster.core.EMConfig;
import org.evomaster.core.problem.rest.HttpVerb;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.emb.json.EMBJsonTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;

public class LanguageServerExampleEMTest extends EMBJsonTestBase {

    @BeforeAll
    public static void initClass() throws Exception {
        LanguageServerExampleController controller = new LanguageServerExampleController();
        EMConfig config = new EMConfig();
        config.getInstrumentMR_EXT_0();
        EMBJsonTestBase.initClass(controller, config);
    }

    @Disabled
    public void runEMTest() throws Throwable {
        runTestHandlingFlakyAndCompilation(
                "LanguageServerExampleEMTest",
                "org.foo.LanguageServerExampleEMTest",
                500,
                true,
                (args) -> {
                    Solution<RestIndividual> solution = initAndRun(args);

                    assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/language/json", "Working");
                },
                3
        );
    }
}
