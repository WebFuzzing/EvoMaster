package com.foo.graphql.base

import com.foo.graphql.SpringController
import org.evomaster.client.java.controller.problem.GraphQlProblem
import org.evomaster.client.java.controller.problem.ProblemInfo
import org.springframework.boot.SpringApplication


class BaseCustomEndpointController : SpringController(GQLBaseApplication::class.java) {

    override fun schemaName() = GQLBaseApplication.SCHEMA_NAME

    override fun startSut(): String {
        ctx = SpringApplication.run(applicationClass,
                "--server.port=0",
                "--graphql.tools.schema-location-pattern=**/${schemaName()}",
                "--graphql.servlet.mapping=/graphqlv2",
                "--graphiql.mapping=/graphiqlv2"
        )
        return "http://localhost:$sutPort"
    }

    override fun getProblemInfo(): ProblemInfo {
        return GraphQlProblem("/graphqlv2")
    }
}