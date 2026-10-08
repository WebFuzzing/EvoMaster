package org.evomaster.e2etests.spring.rest.polyglotpersistence

import com.foo.spring.rest.polyglotpersistence.PolyglotPersistenceSutController
import io.restassured.RestAssured
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PolyglotPersistenceControllerTest {

    private lateinit var controller: PolyglotPersistenceSutController
    private lateinit var baseUrl: String

    @BeforeEach
    fun setUp() {
        controller = PolyglotPersistenceSutController()
        baseUrl = controller.startSut()
    }

    @AfterEach
    fun tearDown() {
        controller.stopSut()
    }

    @Test
    fun testCanStartSutAndAccessEndpoints() {
        assertTrue(controller.isSutRunning())

        // Initial combined GET returns 400 (no data)
        RestAssured.given().get("$baseUrl/api/get/42/42/42").then().statusCode(400)

        // Add to Mongo only -> still 400
        RestAssured.given().post("$baseUrl/api/mongo/42").then().statusCode(200)
        RestAssured.given().get("$baseUrl/api/get/42/42/42").then().statusCode(400)

        // Add to Redis only -> still 400
        RestAssured.given().post("$baseUrl/api/redis/42").then().statusCode(200)
        RestAssured.given().get("$baseUrl/api/get/42/42/42").then().statusCode(400)

        // Add to Postgres -> now all 3 are present -> 200
        RestAssured.given().post("$baseUrl/api/postgres/42").then().statusCode(200)
        RestAssured.given().get("$baseUrl/api/get/42/42/42").then().statusCode(200)
    }
}
