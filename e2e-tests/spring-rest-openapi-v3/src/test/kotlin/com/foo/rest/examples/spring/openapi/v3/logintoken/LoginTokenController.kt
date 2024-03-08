package com.foo.rest.examples.spring.openapi.v3.logintoken

import com.foo.rest.examples.spring.openapi.v3.SpringController
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto
import org.evomaster.client.java.controller.api.dto.auth.JsonTokenPostLoginDto
import org.evomaster.client.java.controller.api.dto.auth.LoginEndpointDto
import org.evomaster.client.java.controller.problem.ProblemInfo
import org.evomaster.client.java.controller.problem.RestProblem

class LoginTokenController: SpringController(LoginTokenApplication::class.java){


    override fun getProblemInfo(): ProblemInfo {
        return RestProblem(
                "http://localhost:$sutPort/v3/api-docs",
                listOf("/api/logintoken/login") // make sure it is handled in auth
        )
    }

    override fun getInfoForAuthentication(): List<AuthenticationDto> {
        return listOf(
                AuthenticationDto("Foo")
                    .apply { loginEndpointAuth = LoginEndpointDto()
                    .apply {
                    endpoint = "/api/logintoken/login"
                    payload = """
                        {"userId": "foo", "password":"123"}
                    """.trimIndent()
                    extractTokenFromJSONField = "/token/authToken"
                    contentType = "application/json"
                    httpHeaderName = "Authorization"
                    headerPrefix = "Bearer "
                } }
        )
    }
}