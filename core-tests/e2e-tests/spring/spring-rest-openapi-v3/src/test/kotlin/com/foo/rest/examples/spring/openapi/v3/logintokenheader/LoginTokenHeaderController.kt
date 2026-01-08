package com.foo.rest.examples.spring.openapi.v3.logintokenheader

import com.foo.rest.examples.spring.openapi.v3.SpringController
import com.webfuzzing.commons.auth.Header
import com.webfuzzing.commons.auth.LoginEndpoint
import com.webfuzzing.commons.auth.TokenHandling
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto
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

        val le = LoginEndpoint()
        le.endpoint = "/api/logintokenheader/login"
        le.verb = LoginEndpoint.HttpVerb.POST
        le.contentType = null
        le.expectCookies = false
        le.headers.add(Header().apply { name="Authorization"; value = "foo 123"  })

        le.token = TokenHandling()
        le.token.extractFrom = TokenHandling.ExtractFrom.BODY
        le.token.extractSelector = "/token/authToken"
        le.token.sendTemplate = "Bearer {token}"
        le.token.sendName = "Authorization"
        le.token.sendIn = TokenHandling.SendIn.HEADER

        val dto = AuthenticationDto("foo")
        dto.loginEndpointAuth = le

        return listOf(dto)
    }
}