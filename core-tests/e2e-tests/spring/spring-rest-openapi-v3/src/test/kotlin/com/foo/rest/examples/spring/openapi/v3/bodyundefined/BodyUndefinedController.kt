package com.foo.rest.examples.spring.openapi.v3.bodyundefined

import com.foo.rest.examples.spring.openapi.v3.SpringController
import org.evomaster.client.java.controller.problem.ProblemInfo
import org.evomaster.client.java.controller.problem.RestProblem


class BodyUndefinedController : SpringController(BodyUndefinedApplication::class.java){


    override fun getProblemInfo(): ProblemInfo {
        /*
            SpringDoc generates non-sense schema for this...
            why the fuck transforming a body payload declaration into a query parameter?!?
            https://github.com/springdoc/springdoc-openapi/issues/1003

            note, this adhoc schema here is technically invalid according to
            https://swagger.io/docs/specification/describing-request-body/
         */
        return RestProblem(
            "http://localhost:$sutPort/openapi-bodyundefined.json",
            null
        )
    }
}