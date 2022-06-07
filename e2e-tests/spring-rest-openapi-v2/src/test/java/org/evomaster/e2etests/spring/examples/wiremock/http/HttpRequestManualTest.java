package org.evomaster.e2etests.spring.examples.wiremock.http;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.core.Is.is;

public class HttpRequestManualTest extends HttpRequestTestBase {

    /**
     * TODO: Can be removed
     */

    @Disabled
    public void testURLConnection() {
        given().accept(ContentType.JSON)
                .get(baseUrlOfSut + "/api/wiremock/external/url")
                .then()
                .statusCode(200)
                .body("valid", is(true));
    }

    @Disabled
    public void testURLConnectionWithQuery() {
        given().accept(ContentType.JSON)
                .get(baseUrlOfSut + "/api/wiremock/external/url/withQuery")
                .then()
                .statusCode(200)
                .body("valid", is(true));
    }

    @Disabled
    public void testHttpClient() {
        // Note: Test for Java 11 HttpClient not implemented

        given().accept(ContentType.JSON)
                .get(baseUrlOfSut + "/api/wiremock/external/http")
                .then()
                .statusCode(200)
                .body("valid", is(true));
    }

    @Disabled
    public void testApacheHttpClient() {
        given().accept(ContentType.JSON)
                .get(baseUrlOfSut + "/api/wiremock/external/apache")
                .then()
                .statusCode(200)
                .body("valid", is(true));
    }

    @Disabled
    public void testOkHttpClient() {
        given().accept(ContentType.JSON)
                .get(baseUrlOfSut + "/api/wiremock/external/okhttp")
                .then()
                .statusCode(200)
                .body("valid", is(true));
    }
}
