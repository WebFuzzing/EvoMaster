package com.foo.rest.examples.spring.openapi.v3.logincreateusers

import com.foo.rest.examples.spring.openapi.v3.SpringController
import com.foo.rest.examples.spring.openapi.v3.logincreateuser.LoginCreateUsersApplication
import com.foo.rest.examples.spring.openapi.v3.logintoken.LoginTokenApplication
import org.evomaster.client.java.controller.AuthUtils
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto
import org.evomaster.client.java.controller.problem.ProblemInfo
import org.evomaster.client.java.controller.problem.RestProblem

class LoginCreateUsersController : SpringController(LoginCreateUsersApplication::class.java) {


    override fun getProblemInfo(): ProblemInfo {
        return RestProblem(
            "http://localhost:$sutPort/v3/api-docs",
            null
        )
    }
}