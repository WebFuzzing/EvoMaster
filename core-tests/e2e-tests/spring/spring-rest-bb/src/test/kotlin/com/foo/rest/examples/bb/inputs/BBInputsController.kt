package com.foo.rest.examples.bb.inputs

import com.foo.rest.examples.bb.SpringController
import org.evomaster.client.java.controller.problem.ProblemInfo
import org.evomaster.client.java.controller.problem.RestProblem

class BBInputsController : SpringController(BBInputsApplication::class.java){


    override fun getProblemInfo(): ProblemInfo {
        return RestProblem(
            "http://localhost:$sutPort/openapi-bbinputs.json",
            null
        )
    }
}