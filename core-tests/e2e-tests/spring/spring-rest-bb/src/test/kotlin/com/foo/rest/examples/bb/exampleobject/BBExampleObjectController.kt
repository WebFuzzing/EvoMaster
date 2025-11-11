package com.foo.rest.examples.bb.exampleobject

import com.foo.rest.examples.bb.SpringController
import org.evomaster.client.java.controller.problem.ProblemInfo
import org.evomaster.client.java.controller.problem.RestProblem

class BBExampleObjectController : SpringController(BBExampleObjectApplication::class.java){


    override fun getProblemInfo(): ProblemInfo {
        return RestProblem(
            "http://localhost:$sutPort/openapi-bbexampleobject.json",
            null
        )
    }
}