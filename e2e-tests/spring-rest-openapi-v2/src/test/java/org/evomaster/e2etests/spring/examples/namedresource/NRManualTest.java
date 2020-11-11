package org.evomaster.e2etests.spring.examples.namedresource;

import com.foo.rest.examples.spring.namedresource.NamedResourceDto;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.core.Is.is;

/**
 * Created by arcand on 01.03.17.
 */
public class NRManualTest extends NRTestBase{

    @Test
    public void testPostGet() {

        String name = "aName";
        String value = "foo";

        createWithPost(name, value);

        given().accept(ContentType.JSON)
                .get(baseUrlOfSut + "/api/nr/"+name)
                .then()
                .statusCode(200)
                .body("value", is(value));
    }

    private void createWithPost(String name, String value){
        given().contentType(ContentType.JSON)
                .body(new NamedResourceDto(name, value))
                .post(baseUrlOfSut + "/api/nr/"+name)
                .then()
                .statusCode(201);
    }

    @Test
    public void testPostDelete(){

        String name = "bar";
        String value = "xyz";

        createWithPost(name, value);

        given().delete(baseUrlOfSut + "/api/nr/"+name)
                .then()
                .statusCode(204);
    }

    @Test
    public void testPostPut(){

        String name = "post";
        String value = "put";

        createWithPost(name, value);

        given().contentType(ContentType.JSON)
                .body(new NamedResourceDto(name, value))
                .put(baseUrlOfSut + "/api/nr/"+name)
                .then()
                .statusCode(204);
    }

    @Test
    public void testPostPatch(){

        String name = "post";
        String value = "patch";

        createWithPost(name, value);

        given().contentType(ContentType.JSON)
                .body(new NamedResourceDto(name, value))
                .patch(baseUrlOfSut + "/api/nr/"+name)
                .then()
                .statusCode(204);
    }

    @Test
    public void testSingleGet(){

        given().accept(ContentType.JSON)
                .get(baseUrlOfSut + "/api/nr/someName")
                .then()
                .statusCode(404);
    }

    @Test
    public void testSingleDelete(){

        given().delete(baseUrlOfSut + "/api/nr/someName")
                .then()
                .statusCode(404);
    }

    @Test
    public void testSinglePatch(){

        String name = "missingElement";
        String value = "patch";

        given().contentType(ContentType.JSON)
                .body(new NamedResourceDto(name, value))
                .patch(baseUrlOfSut + "/api/nr/" + name)
                .then()
                .statusCode(404);
    }

    @Test
    public void testSinglePut(){

        String name = "createWithPut";
        String value = "put";

        given().contentType(ContentType.JSON)
                .body(new NamedResourceDto(name, value))
                .put(baseUrlOfSut + "/api/nr/"+name)
                .then()
                .statusCode(201);
    }

}
