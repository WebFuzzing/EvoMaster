package com.foo.rest.examples.bb.bodyunsupported

import com.foo.rest.examples.bb.SpringController
import org.evomaster.client.java.controller.problem.ProblemInfo
import org.evomaster.client.java.controller.problem.RestProblem

class BBBodyUnsupportedController : SpringController(BBBodyUnsupportedApplication::class.java){


    override fun getProblemInfo(): ProblemInfo {
        return RestProblem(
            "http://localhost:$sutPort/openapi-bodyunsupported.json",
            null
        )
    }
}