package org.evomaster.e2etests.spring.rest.mongo.document;

import com.foo.spring.rest.mongo.BsonDocumentAppController;
import org.evomaster.core.EMConfig;
import org.evomaster.core.problem.rest.data.HttpVerb;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.utils.RestTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class BsonDocumentWithoutMREMTest extends RestTestBase {

    @BeforeAll
    public static void initClass() throws Exception {
        EMConfig config = new EMConfig();
        config.setInstrumentMR_MONGO(false);
        RestTestBase.initClass(new BsonDocumentAppController(), config);
    }


    @Test
    public void testRunEMWithoutMongoMethodReplacement() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "BsonDocumentWithoutMREM",
                1000,
                (args) -> {
                    setOption(args,"enableWeightBasedMutationRateSelectionForGene","false");
                    setOption(args,"heuristicsForMongo","false");
                    setOption(args,"instrumentMR_MONGO","false");
                    setOption(args,"generateMongoData","false");
                    setOption(args,"extractMongoExecutionInfo","false");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertFalse(solution.getIndividuals().isEmpty());
                    assertNone(solution, HttpVerb.GET, 200, "/bsondocument/parse", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/bsondocument/parse", null);
                });
    }
}
