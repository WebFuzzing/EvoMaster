package com.foo.rest.examples.bb.linksmulti

import com.foo.rest.examples.bb.SpringController
import org.evomaster.client.java.controller.problem.ProblemInfo
import org.evomaster.client.java.controller.problem.RestProblem

class BBLinksMultiController : SpringController(BBLinksMultiApplication::class.java) {

    override fun getProblemInfo(): ProblemInfo {
        return RestProblem(
            "http://localhost:$sutPort/openapi-linksmulti.json", null
        )
    }

}