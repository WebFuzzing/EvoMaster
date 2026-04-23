package com.foo.rest.examples.spring.openapi.v3.enum

import com.foo.rest.examples.spring.openapi.v3.SpringController
import org.evomaster.client.java.controller.problem.ProblemInfo
import org.evomaster.client.java.controller.problem.RestProblem

class EnumController : SpringController(EnumApplication::class.java){


    override fun getProblemInfo(): ProblemInfo {
        return RestProblem(
                "http://localhost:$sutPort/openapi-enum.yml",
                null
        )
    }
}