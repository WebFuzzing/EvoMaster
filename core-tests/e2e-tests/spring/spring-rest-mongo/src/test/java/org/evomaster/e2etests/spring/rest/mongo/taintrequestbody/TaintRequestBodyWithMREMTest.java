package org.evomaster.e2etests.spring.rest.mongo.taintrequestbody;

import com.foo.spring.rest.mongo.TaintRequestBodyAppController;
import org.evomaster.core.EMConfig;
import org.evomaster.core.problem.rest.data.HttpVerb;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.utils.RestTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class TaintRequestBodyWithMREMTest extends RestTestBase {

    @BeforeAll
    public static void initClass() throws Exception {
        EMConfig config = new EMConfig();
        config.setInstrumentMR_MONGO(true);
        RestTestBase.initClass(new TaintRequestBodyAppController(), config);
    }

    @Test
    public void testRequestBodyWithoutCompilation() throws Throwable {

        runTestHandlingFlaky(
                "TaintRequestBodyWithMREM",
                "org.foo.spring.rest.mongo.TaintRequestBodyEMGeneration",
                1000,
                true,
                (args) -> {
                    setOption(args, "taintForceSelectionOfGenesWithSpecialization", "true");
                    setOption(args,"discoveredInfoRewardedInFitness", "true");


                    setOption(args,"heuristicsForMongo","false");
                    setOption(args,"instrumentMR_MONGO","true");
                    setOption(args,"generateMongoData","false");
                    setOption(args,"extractMongoExecutionInfo","false");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertFalse(solution.getIndividuals().isEmpty());
                    assertHasAtLeastOne(solution, HttpVerb.POST, 400, "/taintrequestbody/getStringRequestBody", null);
                    assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/taintrequestbody/getStringRequestBody", null);
                });
    }

    @Test
    public void testRequestBodyWithTestCompilation() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "TaintRequestBodyWithMREM",
                "org.foo.spring.rest.mongo.TaintRequestBodyEMGeneration",
                1000,
                (args) -> {
                    setOption(args, "taintForceSelectionOfGenesWithSpecialization", "true");
                    setOption(args,"discoveredInfoRewardedInFitness", "true");

                    setOption(args,"heuristicsForMongo","false");
                    setOption(args,"instrumentMR_MONGO","true");
                    setOption(args,"generateMongoData","false");
                    setOption(args,"extractMongoExecutionInfo","false");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertFalse(solution.getIndividuals().isEmpty());
                    assertHasAtLeastOne(solution, HttpVerb.POST, 400, "/taintrequestbody/getStringRequestBody", null);
                    assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/taintrequestbody/getStringRequestBody", null);
                });
    }


}
