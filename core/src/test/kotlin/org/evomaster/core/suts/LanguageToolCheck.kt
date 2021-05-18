package org.evomaster.core.suts

import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import org.junit.jupiter.api.Test



class LanguageToolCheck {


    //@Test
    fun test(){

        /*
         *  Test used to evaluate performance hit of instrumentation on LanguageTool
         */


        val payload = """
            text=evomaster_261_input&language=auto&enabledRules=evomaster_262_input&disabledCategories=PFF_6Ir9a2ZT8J
        """.trimIndent()

        given().contentType(ContentType.URLENC)
                .body(payload)
                .port(53985)
//                .port(8081)
                .post("/v2/check")
                .then()
                .statusCode(200)
    }
}