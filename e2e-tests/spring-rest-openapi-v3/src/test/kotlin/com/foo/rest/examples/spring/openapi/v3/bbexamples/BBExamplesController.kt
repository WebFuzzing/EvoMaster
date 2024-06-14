package com.foo.rest.examples.spring.openapi.v3.bbexamples

import com.foo.rest.examples.spring.openapi.v3.SpringController
import org.evomaster.client.java.controller.problem.ProblemInfo
import org.evomaster.client.java.controller.problem.RestProblem

class BBExamplesController : SpringController(BBExamplesApplication::class.java){


    override fun getProblemInfo(): ProblemInfo {
        return RestProblem(
                "http://localhost:$sutPort/openapi-bbexamples.json",
                null
        )
    }
}