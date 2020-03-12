package org.evomaster.e2etests.spring.rest.mongo.foo;

import com.foo.mongo.MyMongoAppEmbeddedController;
import com.foo.mongo.person.PersonDto;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.mongodb.client.FindIterable;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import org.bson.Document;
import org.evomaster.client.java.controller.internal.db.StandardOutputTracker;
import org.evomaster.core.Main;
import org.evomaster.core.database.DbAction;
import org.evomaster.core.problem.rest.*;
import org.evomaster.core.problem.rest.auth.NoAuth;
import org.evomaster.core.problem.rest.param.Param;
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

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    private static ValidatableResponse add(PersonDto johnDoeDto) {
        return given()
                .contentType(ContentType.JSON)
                .body(johnDoeDto)
                .post(baseUrlOfSut + "/api/mongoperson/add")
                .then()
                .statusCode(200);
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
        assertTrue(fv.averageExtraDistancesToMinimize(0) == Double.MAX_VALUE);

    }

    @Test
    public void testExtraFitnessWithSomeDocuments() {

        String[] args = new String[]{
                "--createTests", "true",
                "--seed", "42",
                "--sutControllerPort", "" + controllerPort,
                "--maxActionEvaluations", "2",
                "--stoppingCriterion", "FITNESS_EVALUATIONS",
                "--heuristicsForMongo", "true",
                "--maxTestSize", "2"
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

        RestCallAction postAction = new RestCallAction("POST/api/mongoperson/addJoeBlack",
                HttpVerb.POST, new RestPath("/api/mongoperson/addJoeBlack"),
                new LinkedList<Param>(),
                new NoAuth(),
                false,
                null,
                new LinkedList<String>(),
                new HashMap<String, String>());

        RestCallAction getAction = new RestCallAction("GET/api/mongoperson/findByAge/{age}",
                HttpVerb.GET,
                new RestPath("/api/mongoperson/findByAge/{age}"),
                Arrays.asList(pathParam),
                new NoAuth(),
                false,
                null,
                new LinkedList<String>(),
                new HashMap<String, String>());

        RestIndividual individual = new RestIndividual(Arrays.asList(postAction, getAction),
                SampleType.RANDOM,
                new LinkedList<DbAction>(),
                null,
                null);

        FindIterable<Document> findIterable0 = sutController.getMongoClient().getDatabase("testdb").getCollection("person").find();
        assertFalse(findIterable0.iterator().hasNext());

        EvaluatedIndividual ei = ff.calculateCoverage(individual);
        FitnessValue fv = ei.getFitness();

        //as no data in database meets criterion, should get distance greater than zero
        assertTrue(fv.averageExtraDistancesToMinimize(1) > 0);

//        FindIterable<Document> findIterable1 = sutController.getMongoClient().getDatabase("testdb").getCollection("person").find();
//        assertFalse(findIterable1.iterator().hasNext());

    }


}