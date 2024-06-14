package org.evomaster.e2etests.spring.rest.mongo.reservations;

import com.foo.spring.rest.mongo.MongoPersonsAppController;
import com.foo.spring.rest.mongo.MongoPersonsWithoutPostAppController;
import com.foo.spring.rest.mongo.ReservationsAppController;
import org.evomaster.core.EMConfig;
import org.evomaster.core.problem.rest.HttpVerb;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.utils.RestTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ReservationsEMTest extends RestTestBase {

    @BeforeAll
    public static void initClass() throws Exception {
        EMConfig config = new EMConfig();
        config.setInstrumentMR_MONGO(true);
        RestTestBase.initClass(new ReservationsAppController(), config);
    }

    @Test
    public void testRunEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "MongoReservationsEM",
                "org.foo.spring.rest.mongo.MongoEMFitness",
                10000,
                (args) -> {
                    args.add("--heuristicsForMongo");
                    args.add("true");
                    args.add("--instrumentMR_MONGO");
                    args.add("true");
                    args.add("--generateMongoData");
                    args.add("true");
                    args.add("--extractMongoExecutionInfo");
                    args.add("true");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/reservations/findAll", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/reservations/findAll", null);


                });
    }
}