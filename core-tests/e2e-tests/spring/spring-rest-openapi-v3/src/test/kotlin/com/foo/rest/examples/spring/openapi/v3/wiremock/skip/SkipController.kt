package com.foo.rest.examples.spring.openapi.v3.wiremock.skip

import com.foo.rest.examples.spring.openapi.v3.SpringController
import org.evomaster.client.java.controller.problem.ExternalService
import org.evomaster.client.java.controller.problem.ProblemInfo
import org.evomaster.client.java.controller.problem.RestProblem

class SkipController : SpringController(SkipApplication::class.java) {

    override fun getProblemInfo(): ProblemInfo {
        val services: MutableList<ExternalService> = mutableListOf()
        services.add(ExternalService("darpa.int", 8080))

        return RestProblem(
            "http://localhost:$sutPort/v3/api-docs",
            null
        ).withServicesToNotMock(services)
    }
}