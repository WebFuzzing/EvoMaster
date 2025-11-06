package org.evomaster.e2etests.spring.examples.db.preparedstatement;

import com.foo.rest.examples.spring.db.preparedstatement.PreparedStatementController;
import io.restassured.http.ContentType;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

public class PreparedStatementTest extends PreparedStatementTestBase {

    @Test
    public void testEmpty(){

        given().accept(ContentType.ANY)
                .get(baseUrlOfSut + "/api/db/preparedStatement/42/BAR/false")
                .then()
                .statusCode(400);
    }

    @Test
    public void testNonEmpty(){

        given().accept(ContentType.ANY)
                .post(baseUrlOfSut + "/api/db/preparedStatement/")
                .then()
                .statusCode(200);


        given().accept(ContentType.ANY)
                .get(baseUrlOfSut + "/api/db/preparedStatement/42/BAR/false")
                .then()
                .statusCode(200);
    }


}
