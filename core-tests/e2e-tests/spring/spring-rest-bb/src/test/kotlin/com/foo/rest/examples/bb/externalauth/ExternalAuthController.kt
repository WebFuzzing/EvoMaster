package com.foo.rest.examples.bb.externalauth

import com.foo.rest.examples.bb.SpringController
import org.evomaster.client.java.controller.problem.ProblemInfo
import org.evomaster.client.java.controller.problem.RestProblem

class ExternalAuthController : SpringController(ExternalAuthApplication::class.java) {

    override fun getProblemInfo(): ProblemInfo {
        return RestProblem(
            "http://localhost:$sutPort/v3/api-docs",
            listOf("/api/externalauth/login1","/api/externalauth/login2")
        )
    }

}