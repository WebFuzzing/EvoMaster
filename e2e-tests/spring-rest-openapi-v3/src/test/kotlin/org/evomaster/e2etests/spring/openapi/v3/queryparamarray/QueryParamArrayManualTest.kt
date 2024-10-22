package org.evomaster.e2etests.spring.openapi.v3.queryparamarray

import com.foo.rest.examples.spring.openapi.v3.queryparamarray.QueryParamArrayController
import io.restassured.RestAssured
import io.restassured.RestAssured.given
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test


class QueryParamArrayManualTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(QueryParamArrayController())
        }
    }

    @Test
    fun testArray(){

        //RestAssured.enableLoggingOfRequestAndResponseIfValidationFails()

        given().accept("*/*")
            .get("$baseUrlOfSut/api/queryparamarray?x=")
            .then()
            .statusCode(400)

        var body = given().accept("*/*")
            .get("$baseUrlOfSut/api/queryparamarray?x=1")
            .then()
            .statusCode(200)
            .extract().asString()
        assertEquals("[1]", body)


        body = given().accept("*/*")
            .get("$baseUrlOfSut/api/queryparamarray?x=1&x=2")
            .then()
            .statusCode(200)
            .extract().asString()
        assertEquals("[1,2]", body)

        body = given().accept("*/*")
            .get("$baseUrlOfSut/api/queryparamarray?x=1&x=1")
            .then()
            .statusCode(200)
            .extract().asString()
        assertEquals("[1,1]", body)

        body = given().accept("*/*")
            .get("$baseUrlOfSut/api/queryparamarray?x=1,2")
            .then()
            .statusCode(200)
            .extract().asString()
        assertEquals("[1,2]", body)


        //Spring does not seem to allow use different delimeters, such as " " and "|", unless custom code is written

//        body = given().accept("*/*")
//            .get(baseUrlOfSut + "/api/queryparamarray?x=1 2")
//            .then()
//            .statusCode(200)
//            .extract().asString()
//        assertEquals("[1,2]", body)


//        body = given().accept("*/*")
//            .get(baseUrlOfSut + "/api/queryparamarray?x=1|2")
//            .then()
//            .statusCode(200)
//            .extract().asString()
//        assertEquals("[1,2]", body)
    }
}