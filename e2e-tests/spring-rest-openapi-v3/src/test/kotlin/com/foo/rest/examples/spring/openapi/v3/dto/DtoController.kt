package com.foo.rest.examples.spring.openapi.v3.dto

import com.foo.rest.examples.spring.openapi.v3.SpringController
import org.evomaster.client.java.controller.problem.ProblemInfo
import org.evomaster.client.java.controller.problem.RestProblem

class DtoController : SpringController(DtoApplication::class.java){


    override fun getProblemInfo(): ProblemInfo {
        return RestProblem(
            "http://localhost:$sutPort/openapi-dto.yml",
            null
        )
    }
}
