package com.foo.rest.examples.spring.openapi.v3.headerobject

import com.foo.rest.examples.spring.openapi.v3.SpringController
import org.evomaster.client.java.controller.problem.ProblemInfo
import org.evomaster.client.java.controller.problem.RestProblem

class HeaderObjectController : SpringController(HeaderObjectApplication::class.java){

    override fun getProblemInfo(): ProblemInfo {
        return RestProblem(
                "http://localhost:$sutPort/openapi-headerobject.json",
                null
        )
    }

}