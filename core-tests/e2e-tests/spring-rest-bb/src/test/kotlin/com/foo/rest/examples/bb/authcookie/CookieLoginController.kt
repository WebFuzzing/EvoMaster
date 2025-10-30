package com.foo.rest.examples.bb.authcookie

import com.foo.rest.examples.bb.SpringController
import org.evomaster.client.java.controller.problem.ProblemInfo
import org.evomaster.client.java.controller.problem.RestProblem

class CookieLoginController : SpringController(CookieLoginApplication::class.java) {

    override fun getProblemInfo(): ProblemInfo {
        return RestProblem(
            "http://localhost:$sutPort/v3/api-docs",
            listOf("/api/logintoken/login")
        )
    }

}