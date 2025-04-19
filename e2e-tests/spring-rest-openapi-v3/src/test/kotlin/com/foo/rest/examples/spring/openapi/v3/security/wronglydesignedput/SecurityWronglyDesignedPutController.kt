package com.foo.rest.examples.spring.openapi.v3.security.wronglydesignedput


import com.foo.rest.examples.spring.openapi.v3.SpringController
import org.evomaster.client.java.controller.AuthUtils
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto
import org.evomaster.client.java.controller.problem.ProblemInfo
import org.evomaster.client.java.controller.problem.RestProblem

class SecurityWronglyDesignedPutController : SpringController(SecurityWronglyDesignedPut::class.java){

    override fun getProblemInfo(): ProblemInfo {
        return RestProblem(
            "http://localhost:$sutPort/v3/api-docs", null
        )
    }
}