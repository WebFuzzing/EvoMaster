package com.foo.rest.examples.spring.openapi.v3.dtoreflectiveassert

import com.foo.rest.examples.spring.openapi.v3.SpringController
import org.evomaster.client.java.controller.problem.ProblemInfo
import org.evomaster.client.java.controller.problem.RestProblem

class DtoReflectiveAssertController : SpringController(DtoReflectiveAssertApplication::class.java){

    override fun getProblemInfo(): ProblemInfo {
        return RestProblem(
            "http://localhost:$sutPort/openapi-dto-reflective-assert.yaml",
            null
        )
    }
}
