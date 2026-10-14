package org.evomaster.e2etests.spring.rest.mongolegacy.persons;

import com.foo.spring.rest.mongolegacy.LegacyMongoPersonsAppController;
import org.evomaster.core.EMConfig;
import org.evomaster.core.problem.rest.data.HttpVerb;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.utils.RestTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Same as other Mongo E2E tests, but with the SUT using the legacy MongoDB driver 3.8
 */
public class LegacyMongoPersonsEMTest extends RestTestBase {

    @BeforeAll
    public static void initClass() throws Exception {
        EMConfig config = new EMConfig();
        config.setInstrumentMR_MONGO(true);
        RestTestBase.initClass(new LegacyMongoPersonsAppController(), config);
    }

    @Test
    public void testRunEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "LegacyMongoPersonsEM",
                2000,
                (args) -> {
                    setOption(args, "heuristicsForMongo", "true");
                    setOption(args, "instrumentMR_MONGO", "true");
                    setOption(args, "generateMongoData", "false");
                    setOption(args, "extractMongoExecutionInfo", "true");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertFalse(solution.getIndividuals().isEmpty());
                    assertHasAtLeastOne(solution, HttpVerb.POST, 201, "/legacymongo/persons", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 404, "/legacymongo/persons", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/legacymongo/persons", null);
                });
    }
}
