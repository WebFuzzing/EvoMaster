package org.evomaster.e2etests.spring.examples.db.auth;

import io.restassured.http.ContentType;
import org.evomaster.client.java.controller.api.dto.database.operations.InsertionDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.evomaster.client.java.controller.db.dsl.SqlDsl.sql;
import static org.hamcrest.CoreMatchers.equalTo;

public class DbAuthManualTest extends DbAuthTestBase{

    @Test
    public void testGetUsers() {

        String url = baseUrlOfSut + "/api/db/auth/users";


        given().accept(ContentType.JSON)
                .get(url)
                .then()
                .statusCode(200)
                .body("size()", equalTo(1))
                .body("[0].userId", equalTo("foo"))
                .body("[0].password", equalTo("123"));
    }

    @Test
    public void testUnauthorized(){

        String url = baseUrlOfSut + "/api/db/auth/projects";

        given().accept(ContentType.JSON)
                .get(url)
                .then()
                .statusCode(401);
    }


    @Test
    public void testAuthorizedButNoData(){

        String url = baseUrlOfSut + "/api/db/auth/projects";

        given().accept(ContentType.JSON)
                .auth().basic("foo","123")
                .get(url)
                .then()
                .statusCode(400);
    }


    @Test
    public void testAuthorizedWithData(){

        List<InsertionDto> dtos = sql()
                .insertInto("Auth_Project_Entity")
                .d("name", "\"whatever\"")
                .d("owner_user_id ", "\"foo\"")
                .dtos();

        controller.execInsertionsIntoDatabase(dtos);

        String url = baseUrlOfSut + "/api/db/auth/projects";

        given().accept(ContentType.JSON)
                .auth().basic("foo","123")
                .get(url)
                .then()
                .statusCode(200);
    }
}
