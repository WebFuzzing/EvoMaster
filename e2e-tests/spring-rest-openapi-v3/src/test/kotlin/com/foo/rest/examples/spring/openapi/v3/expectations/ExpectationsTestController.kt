package com.foo.rest.examples.spring.openapi.v3.expectations

import com.foo.rest.examples.spring.openapi.v3.SpringController
import org.evomaster.client.java.controller.problem.ProblemInfo
import org.evomaster.client.java.controller.problem.RestProblem

class ExpectationsTestController : SpringController(ExpectationApplication::class.java) {

    val OpenAPI_V2 = "swagger-expectation-test.json"
    val OpenAPI_V3 = "openapi-expectation-test.json"
    override fun getProblemInfo(): ProblemInfo {
        return RestProblem(
                "http://localhost:$sutPort/$OpenAPI_V2",
                null
        )
    }
}