package org.evomaster.e2etests.spring.rest.mongo.customer;

import com.foo.mongo.MyMongoAppEmbeddedController;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import io.swagger.models.Swagger;
import io.swagger.parser.SwaggerParser;
import org.evomaster.client.java.controller.api.Formats;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.controller.internal.db.StandardOutputTracker;
import org.evomaster.core.Main;
import org.evomaster.core.database.DbAction;
import org.evomaster.core.problem.rest.*;
import org.evomaster.core.problem.rest.auth.NoAuth;
import org.evomaster.core.problem.rest.param.Param;
import org.evomaster.core.search.EvaluatedIndividual;
import org.evomaster.core.search.FitnessValue;
import org.evomaster.core.search.service.FitnessFunction;
import org.evomaster.e2etests.spring.rest.mongo.SpringRestMongoTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MongoFooAppTest extends SpringRestMongoTestBase {

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
    public void testSwaggerJSON() {

        SutInfoDto dto = remoteController.getSutInfo();

        String swaggerJson = given().accept(Formats.JSON_V1)
                .get(dto.restProblem.swaggerJsonUrl)
                .then()
                .statusCode(200)
                .extract().asString();

        Swagger swagger = new SwaggerParser().parse(swaggerJson);

        assertEquals("/", swagger.getBasePath());
        assertEquals(3, swagger.getPaths().size());
    }

    @Test
    public void testManualPostAndThenGet() {
        String url = baseUrlOfSut + "/api/mymongoapp/foo";

        given().post(url)
                .then()
                .statusCode(200);

        given().get(url)
                .then()
                .statusCode(200);
    }

    @Test
    public void testManualGetOnEmpty() {
        String url = baseUrlOfSut + "/api/mymongoapp/foo";

        given()
                .get(url)
                .then()
                .statusCode(404);
    }

    @Test
    public void testManualPostGetDeleteGet() {
        String url = baseUrlOfSut + "/api/mymongoapp/foo";

        given().post(url)
                .then()
                .statusCode(200);

        given().get(url)
                .then()
                .statusCode(200);

        given().delete(url)
                .then()
                .statusCode(200);

        given().get(url)
                .then()
                .statusCode(404);

    }

    @Test
    public void testManualDeleteOnEmpty() {
        String url = baseUrlOfSut + "/api/mymongoapp/foo";

        given()
                .delete(url)
                .then()
                .statusCode(404);
    }


    @Test
    public void testManualDuplicatePost() {
        String url = baseUrlOfSut + "/api/mymongoapp/foo";

        given().post(url)
                .then()
                .statusCode(200);

        given().post(url)
                .then()
                .statusCode(400);
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


        RestCallAction getAction = new RestCallAction("GET/api/mongobar",
                HttpVerb.GET, new RestPath("/api/mongobar"),
                new LinkedList<Param>(),
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

    @Test
    public void testExtraFitnessOneDocumentFound() {

        String[] args = new String[]{
                "--createTests", "true",
                "--seed", "42",
                "--sutControllerPort", "" + controllerPort,
                "--maxActionEvaluations", "2",
                "--stoppingCriterion", "FITNESS_EVALUATIONS",
                "--heuristicsForMongo", "true",
                "--maxTestSize", "1"
        };

        Injector injector = Main.init(args);

        FitnessFunction<RestIndividual> ff = injector.getInstance(Key.get(
                new TypeLiteral<FitnessFunction<RestIndividual>>() {
                }));


        RestCallAction postAction = new RestCallAction("POST/api/mongobar",
                HttpVerb.POST, new RestPath("/api/mongobar"),
                new LinkedList<Param>(),
                new NoAuth(),
                false,
                null,
                new LinkedList<String>(),
                new HashMap<String, String>());


        RestCallAction getAction = new RestCallAction("GET/api/mongobar",
                HttpVerb.GET, new RestPath("/api/mongobar"),
                new LinkedList<Param>(),
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

        EvaluatedIndividual ei = ff.calculateCoverage(individual);
        FitnessValue fv = ei.getFitness();

        //as documents are found, the distance should be zero
        assertEquals(0, fv.averageExtraDistancesToMinimize(1));

    }

    @Test
    public void testExtraFitnessManyGets() {

        String[] args = new String[]{
                "--createTests", "true",
                "--seed", "42",
                "--sutControllerPort", "" + controllerPort,
                "--maxActionEvaluations", "2",
                "--stoppingCriterion", "FITNESS_EVALUATIONS",
                "--heuristicsForMongo", "true",
                "--maxTestSize", "1"
        };

        Injector injector = Main.init(args);

        FitnessFunction<RestIndividual> ff = injector.getInstance(Key.get(
                new TypeLiteral<FitnessFunction<RestIndividual>>() {
                }));

        sutController.enableMongoExtractExecution(true);

        RestCallAction getAction0 = new RestCallAction("GET/api/mongobar",
                HttpVerb.GET, new RestPath("/api/mongobar"),
                new LinkedList<Param>(),
                new NoAuth(),
                false,
                null,
                new LinkedList<String>(),
                new HashMap<String, String>());

        RestCallAction getAction1 = new RestCallAction("GET/api/mongobar",
                HttpVerb.GET, new RestPath("/api/mongobar"),
                new LinkedList<Param>(),
                new NoAuth(),
                false,
                null,
                new LinkedList<String>(),
                new HashMap<String, String>());

        RestIndividual individual = new RestIndividual(Arrays.asList(getAction0, getAction1),
                SampleType.RANDOM,
                new LinkedList<DbAction>(),
                null,
                null);

        EvaluatedIndividual ei = ff.calculateCoverage(individual);
        FitnessValue fv = ei.getFitness();

        //as documents were not found, the distances should be non zero for both actions
        assertTrue(fv.averageExtraDistancesToMinimize(0) > 0);
        assertTrue(fv.averageExtraDistancesToMinimize(1) > 0);

    }

    @Test
    public void testExtraFitnessPostGetDeleteGet() {

        String[] args = new String[]{
                "--createTests", "true",
                "--seed", "42",
                "--sutControllerPort", "" + controllerPort,
                "--maxActionEvaluations", "4",
                "--stoppingCriterion", "FITNESS_EVALUATIONS",
                "--heuristicsForMongo", "true",
                "--maxTestSize", "1"
        };

        Injector injector = Main.init(args);

        FitnessFunction<RestIndividual> ff = injector.getInstance(Key.get(
                new TypeLiteral<FitnessFunction<RestIndividual>>() {
                }));


        RestCallAction postAction = new RestCallAction("POST/api/mongobar",
                HttpVerb.POST, new RestPath("/api/mongobar"),
                new LinkedList<Param>(),
                new NoAuth(),
                false,
                null,
                new LinkedList<String>(),
                new HashMap<String, String>());


        RestCallAction getAction0 = new RestCallAction("GET/api/mongobar",
                HttpVerb.GET, new RestPath("/api/mongobar"),
                new LinkedList<Param>(),
                new NoAuth(),
                false,
                null,
                new LinkedList<String>(),
                new HashMap<String, String>());

        RestCallAction deleteAction = new RestCallAction("DELETE/api/mongobar",
                HttpVerb.DELETE, new RestPath("/api/mongobar"),
                new LinkedList<Param>(),
                new NoAuth(),
                false,
                null,
                new LinkedList<String>(),
                new HashMap<String, String>());

        RestCallAction getAction1 = new RestCallAction("GET/api/mongobar",
                HttpVerb.GET, new RestPath("/api/mongobar"),
                new LinkedList<Param>(),
                new NoAuth(),
                false,
                null,
                new LinkedList<String>(),
                new HashMap<String, String>());


        RestIndividual individual = new RestIndividual(Arrays.asList(postAction, getAction0, deleteAction, getAction1),
                SampleType.RANDOM,
                new LinkedList<DbAction>(),
                null,
                null);

        EvaluatedIndividual ei = ff.calculateCoverage(individual);
        FitnessValue fv = ei.getFitness();

        //as documents were not found, the distances should be non zero for both actions
        assertTrue(fv.averageExtraDistancesToMinimize(1) == 0);
        assertTrue(fv.averageExtraDistancesToMinimize(3) > 0);

    }
}