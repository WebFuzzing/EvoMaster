package org.evomaster.e2etests.spring.rest.mongo.foo;

import com.foo.mongo.MyMongoAppEmbeddedController;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import org.evomaster.client.java.controller.internal.db.StandardOutputTracker;
import org.evomaster.core.Main;
import org.evomaster.core.database.DbAction;
import org.evomaster.core.problem.rest.*;
import org.evomaster.core.problem.rest.auth.NoAuth;
import org.evomaster.core.problem.rest.param.PathParam;
import org.evomaster.core.search.EvaluatedIndividual;
import org.evomaster.core.search.FitnessValue;
import org.evomaster.core.search.gene.DisruptiveGene;
import org.evomaster.core.search.gene.IntegerGene;
import org.evomaster.core.search.service.FitnessFunction;
import org.evomaster.core.search.service.mutator.geneMutation.IntMutationUpdate;
import org.evomaster.e2etests.spring.rest.mongo.SpringRestMongoTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ExtraFitnessMongoQueryTest extends SpringRestMongoTestBase {

    private static final MyMongoAppEmbeddedController sutController = new MyMongoAppEmbeddedController();

    @BeforeAll
    public static void init() throws Exception {
        SpringRestMongoTestBase.initClass(sutController);
    }

    @BeforeEach
    public void turnOnTracker() {
        StandardOutputTracker.setTracker(true, sutController);

    }

    @AfterEach
    public void turnOffTracker() {
        StandardOutputTracker.setTracker(false, sutController);
    }

    @Test
    public void testExtraFitnessNoDocumentsFound() {

        String[] args = new String[]{
                "--createTests", "true",
                "--seed", "42",
                "--sutControllerPort", "" + controllerPort,
                "--maxActionEvaluations", "1",
                "--stoppingCriterion", "FITNESS_EVALUATIONS",
                "--heuristicsForMongo", "true",
                "--maxTestSize", "1"
        };

        Injector injector = Main.init(args);

        FitnessFunction<RestIndividual> ff = injector.getInstance(Key.get(
                new TypeLiteral<FitnessFunction<RestIndividual>>() {
                }));


        IntegerGene ageGene = new IntegerGene("age",
                0,
                Integer.MIN_VALUE,
                Integer.MAX_VALUE,
                new IntMutationUpdate(Integer.MIN_VALUE, Integer.MAX_VALUE, 0, false));

        PathParam pathParam = new PathParam("age",
                new DisruptiveGene<IntegerGene>(
                        "age",
                        ageGene,
                        1.0d
                )
        );


        RestCallAction getAction = new RestCallAction("GET/api/mongoperson/findByAge/{age}",
                HttpVerb.GET,
                new RestPath("/api/mongoperson/findByAge/{age}"),
                Arrays.asList(pathParam),
                new NoAuth(),
                false,
                null,
                new LinkedList<String>(),
                new HashMap<String, String>());

        RestIndividual individual = new RestIndividual(Arrays.asList(getAction),
                SampleType.RANDOM,
                new LinkedList<DbAction>(),
                null,
                null);

        EvaluatedIndividual ei = ff.calculateCoverage(individual);
        FitnessValue fv = ei.getFitness();

        //as no data in database, should get distance greater than zero
        assertTrue(fv.averageExtraDistancesToMinimize(0) > 0.0);

    }


}