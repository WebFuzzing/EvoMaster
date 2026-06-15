package com.foo.rest.examples.bb.advancedformats

import com.foo.rest.examples.bb.SpringController
import org.evomaster.client.java.controller.problem.ProblemInfo
import org.evomaster.client.java.controller.problem.RestProblem

class BBAdvancedFormatsController : SpringController(BBAdvancedFormatsApplication::class.java){


    override fun getProblemInfo(): ProblemInfo {
        return RestProblem(
            "http://localhost:$sutPort/openapi-bbadvancedformats.json",
            null
        )
    }
}