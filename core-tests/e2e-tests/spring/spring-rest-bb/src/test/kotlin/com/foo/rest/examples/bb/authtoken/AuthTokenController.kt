package com.foo.rest.examples.bb.authtoken

import com.foo.rest.examples.bb.SpringController
import org.evomaster.client.java.controller.problem.ProblemInfo
import org.evomaster.client.java.controller.problem.RestProblem

class AuthTokenController : SpringController(LoginTokenApplication::class.java) {

    override fun getProblemInfo(): ProblemInfo {
        return RestProblem(
            "http://localhost:$sutPort/v3/api-docs",
            listOf("/api/logintoken/login")
        )
    }

}