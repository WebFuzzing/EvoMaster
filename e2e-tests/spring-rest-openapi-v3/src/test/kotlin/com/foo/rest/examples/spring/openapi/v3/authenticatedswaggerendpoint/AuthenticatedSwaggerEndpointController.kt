package com.foo.rest.examples.spring.openapi.v3.authenticatedswaggerendpoint

import com.foo.rest.examples.spring.openapi.v3.SpringController
import org.evomaster.client.java.controller.AuthUtils
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto
import org.evomaster.client.java.controller.problem.ProblemInfo
import org.evomaster.client.java.controller.problem.RestProblem



class AuthenticatedSwaggerEndpointController : SpringController(AuthenticatedSwaggerEndpointApplication::class.java) {



    override fun getProblemInfo(): ProblemInfo {
        return RestProblem(
            "http://localhost:$sutPort/openapi-schema",
            listOf("/api/logintoken/login") // make sure it is handled in auth
        )
    }



    override fun getInfoForAuthentication(): List<AuthenticationDto> {
        return listOf(
            AuthUtils.getForJsonTokenBearer(
                "Foo",
                "/api/logintoken/login",
                """
                        {"userId": "userName1", "password":"password1234"}
                    """.trimIndent(),
                "/token/authToken"
            )
        )
    }


}