package com.foo.rest.examples.spring.openapi.v3.security.hiddenaccessible

import com.foo.rest.examples.spring.openapi.v3.SpringController
import org.evomaster.client.java.controller.problem.ProblemInfo
import org.evomaster.client.java.controller.problem.RestProblem

class HiddenAccessibleController : SpringController(HiddenAccessibleApplication::class.java){


    override fun getProblemInfo(): ProblemInfo {
        return RestProblem(
                "http://localhost:$sutPort/openapi-hiddenaccessible.yaml",
                null
        )
    }
}