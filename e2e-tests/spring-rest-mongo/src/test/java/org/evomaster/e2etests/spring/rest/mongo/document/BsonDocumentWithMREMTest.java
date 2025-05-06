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

public class BsonDocumentWithMREMTest extends RestTestBase {

    @BeforeAll
    public static void initClass() throws Exception {
        EMConfig config = new EMConfig();
        config.setInstrumentMR_MONGO(true);
        RestTestBase.initClass(new BsonDocumentAppController(), config);
    }

    @Test
    public void testRunEMWithMongoMethodReplacement() throws Throwable {
        runTestHandlingFlakyAndCompilation(
                "BsonDocumentWithMREM",
                1000,
                (args) -> {
                    setOption(args,"enableWeightBasedMutationRateSelectionForGene","false");
                    setOption(args,"heuristicsForMongo","false");
                    setOption(args,"instrumentMR_MONGO","true");
                    setOption(args,"generateMongoData","false");
                    setOption(args,"extractMongoExecutionInfo","false");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertFalse(solution.getIndividuals().isEmpty());
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/bsondocument/parse", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/bsondocument/parse", null);
                });
    }


}
