package com.foo.rest.examples.spring.openapi.v3.logintokenheader

import com.foo.rest.examples.spring.openapi.v3.SpringController
import org.evomaster.client.java.controller.AuthUtils
import org.evomaster.client.java.controller.api.dto.auth.*
import org.evomaster.client.java.controller.problem.ProblemInfo
import org.evomaster.client.java.controller.problem.RestProblem

class LoginTokenHeaderController : SpringController(LoginTokenHeaderApplication::class.java) {


    override fun getProblemInfo(): ProblemInfo {
        return RestProblem(
            "http://localhost:$sutPort/v3/api-docs",
            listOf("/api/logintokenheader/login") // make sure it is handled in auth
        )
    }

    override fun getInfoForAuthentication(): List<AuthenticationDto> {

        val le = LoginEndpointDto()
        le.endpoint = "/api/logintokenheader/login"
        le.verb = HttpVerb.POST
        le.contentType = null
        le.expectCookies = false
        le.headers.add(HeaderDto().apply { name="Authorization"; value = "foo 123"  })

        le.token = TokenHandlingDto()
        le.token.extractFromField = "/token/authToken"
        le.token.headerPrefix = "Bearer "
        le.token.httpHeaderName = "Authorization"

        val dto = AuthenticationDto("foo")
        dto.loginEndpointAuth = le

        return listOf(dto)
    }
}