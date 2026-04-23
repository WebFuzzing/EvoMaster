package com.foo.rest.examples.spring.openapi.v3.expectations

import com.foo.rest.examples.spring.openapi.v3.SpringController
import org.evomaster.client.java.controller.problem.ProblemInfo
import org.evomaster.client.java.controller.problem.RestProblem

class ExpectationBasicTestController : SpringController(ExpectationBasicApplication::class.java) {

    override fun getProblemInfo(): ProblemInfo {
        return RestProblem(
                "http://localhost:$sutPort/openapi-basic-exp-test.json",
                null
        )
    }
}