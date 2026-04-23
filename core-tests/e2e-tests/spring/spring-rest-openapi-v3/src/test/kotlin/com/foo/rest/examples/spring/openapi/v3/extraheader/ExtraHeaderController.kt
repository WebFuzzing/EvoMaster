package com.foo.rest.examples.spring.openapi.v3.extraheader

import com.foo.rest.examples.spring.openapi.v3.SpringController
import org.evomaster.client.java.controller.problem.ProblemInfo
import org.evomaster.client.java.controller.problem.RestProblem

class ExtraHeaderController : SpringController(ExtraHeaderApplication::class.java) {

    override fun getProblemInfo(): ProblemInfo {
        return RestProblem(
            "http://localhost:$sutPort/openapi-extraheader.json",
            null
        )
    }
}