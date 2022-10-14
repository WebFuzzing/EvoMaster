package com.foo.rest.examples.spring.openapi.v3.wiremock.socketconnect.okhttp3

import com.foo.rest.examples.spring.openapi.v3.SpringController
import org.evomaster.client.java.controller.problem.ProblemInfo
import org.evomaster.client.java.controller.problem.RestProblem

class WmSocketConnectController(val skip : List<String> = listOf()) : SpringController(WmSocketConnectApplication::class.java){

    override fun getProblemInfo(): ProblemInfo {
        return RestProblem(
            "http://localhost:$sutPort/v3/api-docs",
            skip
        )
    }
}