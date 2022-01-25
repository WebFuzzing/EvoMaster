package com.foo.rest.examples.spring.openapi.v3.expectations

import com.foo.rest.examples.spring.openapi.v3.SpringController
import org.evomaster.client.java.controller.problem.ProblemInfo
import org.evomaster.client.java.controller.problem.RestProblem

class ExpectationNumTestController : SpringController(ExpectationNumApplication::class.java){

    val openAPI_v2 = "swagger-num-test.json"
    val openAPI_v3 = "openapi-num-test.json"
    override fun getProblemInfo(): ProblemInfo {
        return RestProblem(
                "http://localhost:$sutPort/$openAPI_v2",
                null
        )
    }
}