package org.evomaster.e2etests.spring.examples.formlogin;

import com.foo.rest.examples.spring.formlogin.FormLoginController;
import io.restassured.RestAssured;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.StringContains.containsString;

/**
 * Created by arcuri82 on 25-Oct-19.
 */
public class FormLoginManualTest  extends SpringTestBase {

    @BeforeAll
    public static void initClass() throws Exception {

        SpringTestBase.initClass(new FormLoginController());

        RestAssured.baseURI = baseUrlOfSut;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }


    @Test
    public void testNoAuth(){

        given().get("/api/formlogin/openToAll")
                .then()
                .statusCode(200);

        given().get("/api/formlogin/forUsers")
                .then()
                .statusCode(401);

        given().get("/api/formlogin/forAdmins")
                .then()
                .statusCode(401);
    }


    @Test
    public void testAdmin(){

        final String cookieName = "JSESSIONID";

        String cookie = given().formParam("username", "admin")
                .formParam("password", "bar")
                .post("/login")
                .then()
                .header("location", not(containsString("error")))
                .cookie(cookieName)
                .statusCode(302)
                .extract().cookie(cookieName);

        given().cookie(cookieName, cookie)
                .get("/api/formlogin/openToAll")
                .then()
                .statusCode(200);

        given().cookie(cookieName, cookie)
                .get("/api/formlogin/forUsers")
                .then()
                .statusCode(200);

        given().cookie(cookieName, cookie)
                .get("/api/formlogin/forAdmins")
                .then()
                .statusCode(200);
    }


    @Test
    public void testUser(){

        final String cookieName = "JSESSIONID";

        String cookie = given().formParam("username", "foo")
                .formParam("password", "123456")
                .post("/login")
                .then()
                .header("location", not(containsString("error")))
                .cookie(cookieName)
                .statusCode(302)
                .extract().cookie(cookieName);

        given().cookie(cookieName, cookie)
                .get("/api/formlogin/openToAll")
                .then()
                .statusCode(200);

        given().cookie(cookieName, cookie)
                .get("/api/formlogin/forUsers")
                .then()
                .statusCode(200);

        given().cookie(cookieName, cookie)
                .get("/api/formlogin/forAdmins")
                .then()
                .statusCode(403);
    }


    private Map<String,String> formLogin(String username, String password){

        return given().formParam("username", username)
                .formParam("password", password)
                .post("/login")
                .then().extract().cookies();
    }

    @Test
    public void testUserExtractedCookie(){

        Map<String,String> cookies = formLogin("foo", "123456");

        given().cookies(cookies)
                .get("/api/formlogin/openToAll")
                .then()
                .statusCode(200);

        given().cookies(cookies)
                .get("/api/formlogin/forUsers")
                .then()
                .statusCode(200);

        given().cookies(cookies)
                .get("/api/formlogin/forAdmins")
                .then()
                .statusCode(403);
    }
}
