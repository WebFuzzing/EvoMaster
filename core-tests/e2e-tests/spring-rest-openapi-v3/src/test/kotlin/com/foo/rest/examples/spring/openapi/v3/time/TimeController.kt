package com.foo.rest.examples.spring.openapi.v3.time

import com.foo.rest.examples.spring.openapi.v3.SpringController
import org.evomaster.client.java.controller.problem.ProblemInfo
import org.evomaster.client.java.controller.problem.RestProblem

class TimeController : SpringController(TimeApplication::class.java){


    override fun getProblemInfo(): ProblemInfo {
        return RestProblem(
                "http://localhost:$sutPort/openapi-time.yml",
                null
        )
    }
}