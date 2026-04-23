package com.foo.rest.examples.bb.examplevalues

import com.foo.rest.examples.bb.SpringController
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