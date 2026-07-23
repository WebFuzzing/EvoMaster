package com.foo.rest.examples.spring.openapi.v3.examplepool

import com.foo.rest.examples.spring.openapi.v3.SpringController
import org.evomaster.client.java.controller.problem.ProblemInfo
import org.evomaster.client.java.controller.problem.RestProblem

class ExamplePoolController : SpringController(ExamplePoolApplication::class.java){


    override fun getProblemInfo(): ProblemInfo {
        return RestProblem(
                "http://localhost:$sutPort/openapi-examplepool.yaml",
                null
        )
    }
}