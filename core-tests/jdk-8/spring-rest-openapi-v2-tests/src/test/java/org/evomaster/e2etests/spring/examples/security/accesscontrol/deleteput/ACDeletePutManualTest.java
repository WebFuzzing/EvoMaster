package org.evomaster.e2etests.spring.examples.security.accesscontrol.deleteput;

import com.foo.rest.examples.spring.security.accesscontrol.deleteput.ACDeletePutController;
import com.foo.rest.examples.spring.security.accesscontrol.deleteput.ACDeletePutDto;
import io.restassured.http.ContentType;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.core.Is.is;

public class ACDeletePutManualTest extends SpringTestBase {

    @BeforeAll
    public static void initClass() throws Exception {

        SpringTestBase.initClass(new ACDeletePutController());
    }

    @BeforeEach
    public void reset(){
        //need this, otherwise tests would not be independent
        remoteController.startANewSearch();
    }

    @Test
    public void testCreateAndGet(){

        String resource = "foo";
        ACDeletePutDto payload = new ACDeletePutDto("a",42, true);

        given().accept(ContentType.JSON)
                .get(baseUrlOfSut + "/api/"+resource)
                .then()
                .statusCode(404);

        given().accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(payload)
                .put(baseUrlOfSut + "/api/"+resource)
                .then()
                .statusCode(401);

        given().accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(payload)
                .auth().basic("creator0","creator_password")
                .put(baseUrlOfSut + "/api/"+resource)
                .then()
                .statusCode(201);

        given().accept(ContentType.JSON)
                .get(baseUrlOfSut + "/api/"+resource)
                .then()
                .statusCode(200)
                .body("stringValue", is("a"));
    }


    @Test
    public void testAccessControl(){

        String resource = "bar";
        ACDeletePutDto payload = new ACDeletePutDto("a",42, true);
        String user0 = "creator0";
        String user1 = "creator1";

        //create with a user
        given().accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(payload)
                .auth().basic(user0,"creator_password")
                .put(baseUrlOfSut + "/api/"+resource)
                .then()
                .statusCode(201);

        //deleting with another user must fail
        given().accept(ContentType.JSON)
                .auth().basic(user1,"creator_password")
                .delete(baseUrlOfSut + "/api/"+resource)
                .then()
                .statusCode(403);

        //updating with another user works, so should be a BUG
        given().accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(new ACDeletePutDto()) //all to empty
                .auth().basic(user1,"creator_password")
                .put(baseUrlOfSut + "/api/"+resource)
                .then()
                .statusCode(204); // in theory should get 403, but we simulate bug in API
                // changed the expected status code to 204 which means successful but no content shown.
    }
}
