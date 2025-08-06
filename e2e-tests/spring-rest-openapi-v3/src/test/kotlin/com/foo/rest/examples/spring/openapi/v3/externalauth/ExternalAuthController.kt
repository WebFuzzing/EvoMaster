package com.foo.rest.examples.spring.openapi.v3.externalauth

import com.foo.rest.examples.spring.openapi.v3.SpringController
import org.evomaster.client.java.controller.AuthUtils
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto
import org.evomaster.client.java.controller.problem.ProblemInfo
import org.evomaster.client.java.controller.problem.RestProblem


class ExternalAuthController : SpringController(ExternalAuthApplication::class.java) {

    override fun getProblemInfo(): ProblemInfo {
        return RestProblem(
            "http://localhost:$sutPort/v3/api-docs",
            listOf("/api/externalauth/login1","/api/externalauth/login2")
        )
    }

    override fun getInfoForAuthentication(): List<AuthenticationDto> {
        return listOf(
            AuthUtils.getForJsonToken("test",
                "http://localhost:$sutPort/api/externalauth/login1",
                "{\"username\":\"foo\", \"password\":\"123\"}",
                "/access_token",
                "",
                "application/json"),
        )
    }
}