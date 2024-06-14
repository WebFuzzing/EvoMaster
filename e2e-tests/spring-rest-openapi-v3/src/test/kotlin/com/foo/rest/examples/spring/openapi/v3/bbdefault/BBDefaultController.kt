package com.foo.rest.examples.spring.openapi.v3.bbdefault

import com.foo.rest.examples.spring.openapi.v3.SpringController
import org.evomaster.client.java.controller.problem.ProblemInfo
import org.evomaster.client.java.controller.problem.RestProblem

class BBDefaultController : SpringController(BBDefaultApplication::class.java){


    override fun getProblemInfo(): ProblemInfo {
        return RestProblem(
                "http://localhost:$sutPort/openapi-bbdefault.json",
                null
        )
    }
}