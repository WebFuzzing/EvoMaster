package com.foo.rest.examples.spring.openapi.v3.externalsref

import com.foo.rest.examples.spring.openapi.v3.SpringController
import org.evomaster.client.java.controller.problem.ProblemInfo
import org.evomaster.client.java.controller.problem.RestProblem
import org.springframework.boot.SpringApplication


class ExternalSrefController : SpringController(ExternalSrefApplication::class.java){

    override fun getProblemInfo(): ProblemInfo {

        return RestProblem(
            "http://localhost:$sutPort/sref/schema/swagger.json",
            null
        )
    }

    override fun startSut(): String {
        //need to hardcode port, as used in the schema static files
        ctx = SpringApplication.run(applicationClass, "--server.port=10189")
        return "http://localhost:$sutPort"
    }

}