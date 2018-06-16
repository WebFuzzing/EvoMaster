package org.evomaster.e2etests.spring.examples.db.directintwithsql;

import io.restassured.http.ContentType;
import org.evomaster.clientJava.controllerApi.dto.database.operations.InsertionDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.evomaster.clientJava.controller.db.dsl.SqlDsl.sql;

public class DbDirectIntWithSqlManualTest extends DbDirectIntWithSqlTestBase {


    @Test
    public void testEmpty() {

        given().accept(ContentType.ANY)
                .get(baseUrlOfSut + "/api/db/directint/42/77")
                .then()
                .statusCode(400);
    }

    @Test
    public void testCreateData() {

        int x = 10;
        int y = 34;

        List<InsertionDto> insertions = sql()
                .insertInto("DB_DIRECT_INT_ENTITY").d("X", ""+x).d("Y", ""+y)
                .dtos();

        controller.execInsertionsIntoDatabase(insertions);

        given().accept(ContentType.ANY)
                .get(baseUrlOfSut + "/api/db/directint/"+x+"/"+y)
                .then()
                .statusCode(200);
    }
}