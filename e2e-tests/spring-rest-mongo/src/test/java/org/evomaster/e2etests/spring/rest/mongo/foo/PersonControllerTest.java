package org.evomaster.e2etests.spring.rest.mongo.foo;

import com.foo.mongo.MyMongoAppEmbeddedController;
import org.evomaster.client.java.controller.internal.db.StandardOutputTracker;
import org.evomaster.e2etests.spring.rest.mongo.SpringRestMongoTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

public class PersonControllerTest extends SpringRestMongoTestBase {

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


}