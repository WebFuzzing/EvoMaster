package com.foo.rest.examples.spring.openapi.v3.httporacle.invalidallow.missing

import com.foo.rest.examples.spring.openapi.v3.SpringController
import org.evomaster.client.java.controller.problem.ProblemInfo
import org.evomaster.client.java.controller.problem.RestProblem


class HttpMissingAllowController : SpringController(HttpMissingAllowApplication::class.java) {

    override fun resetStateOfSUT() {
        HttpMissingAllowApplication.reset()
    }

    override fun getProblemInfo(): ProblemInfo {
        return RestProblem(
            "http://localhost:$sutPort/openapi-missingallow.yml",
            null
        )
    }
}
