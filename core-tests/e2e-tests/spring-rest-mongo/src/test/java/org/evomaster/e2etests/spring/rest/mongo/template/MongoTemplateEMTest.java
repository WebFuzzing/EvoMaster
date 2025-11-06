package org.evomaster.e2etests.spring.rest.mongo.template;

import com.foo.spring.rest.mongo.template.MongoTemplateAppController;
import org.evomaster.core.EMConfig;
import org.evomaster.core.problem.rest.data.HttpVerb;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.utils.RestTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class MongoTemplateEMTest extends RestTestBase {

    @BeforeAll
    public static void initClass() throws Exception {
        EMConfig config = new EMConfig();
        config.setInstrumentMR_MONGO(true);
        RestTestBase.initClass(new MongoTemplateAppController());
    }

    @Test
    public void testRunEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "MongoTemplateEMTest",
                "org.foo.spring.rest.mongo.MongoTemplateEMTest",
                500,
                (args) -> {
                    setOption(args,"heuristicsForMongo","true");
                    setOption(args,"instrumentMR_MONGO","true");
                    setOption(args,"generateMongoData","true");
                    setOption(args,"extractMongoExecutionInfo","true");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);
                    assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/mongotemplate/saveData", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/mongotemplate/findData", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/mongotemplate/findData", null);
                });
    }
}
