package org.evomaster.e2etests.spring.examples.db.base;

import com.foo.rest.examples.spring.db.base.DbBaseDto;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

public class DbBaseManualTest extends DbBaseTestBase {


    @Test
    public void testCreateOne() {

        String url = baseUrlOfSut + "/api/db/base/entities";
        String name = "foo";

        given().accept(ContentType.JSON)
                .get(url)
                .then()
                .statusCode(200)
                .body("size()", is(0));

        String location = "";

        location = given().contentType(ContentType.JSON)
                .body(new DbBaseDto(0l, name))
                .post(url)
                .then()
                .statusCode(201)
                .extract().header("location");

        given().accept(ContentType.JSON)
                .get(baseUrlOfSut + location)
                .then()
                .statusCode(200)
                .body("name", is(name));

        given().accept(ContentType.JSON)
                .get(url)
                .then()
                .statusCode(200)
                .body("size()", is(1));
    }


    @Test
    public void testGetByName(){

        String url = baseUrlOfSut + "/api/db/base/entities";
        String name = "foo";

        given().accept(ContentType.JSON)
                .get(url)
                .then()
                .statusCode(200)
                .body("size()", is(0));

        given().accept(ContentType.JSON)
                .get(url+"ByName/"+name)
                .then()
                .statusCode(404);


        given().contentType(ContentType.JSON)
                .body(new DbBaseDto(0l, name))
                .post(url)
                .then()
                .statusCode(201);

        given().contentType(ContentType.JSON)
                .body(new DbBaseDto(0l, name))
                .post(url)
                .then()
                .statusCode(201);

        given().contentType(ContentType.JSON)
                .body(new DbBaseDto(0l, "anotherName"))
                .post(url)
                .then()
                .statusCode(201);


        given().accept(ContentType.JSON)
                .get(url+"ByName/"+name)
                .then()
                .statusCode(200)
                .body("size()", is(2));
    }
}
