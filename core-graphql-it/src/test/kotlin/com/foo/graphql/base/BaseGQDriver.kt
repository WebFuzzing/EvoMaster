package com.foo.graphql.base

import org.evomaster.client.java.controller.EmbeddedSutController
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto
import org.evomaster.client.java.controller.api.dto.SutInfoDto
import org.evomaster.client.java.sql.DbSpecification
import org.evomaster.client.java.controller.problem.GraphQlProblem
import org.evomaster.client.java.controller.problem.ProblemInfo
import org.springframework.boot.SpringApplication
import org.springframework.context.ConfigurableApplicationContext


class BaseGQDriver : EmbeddedSutController(){


    init {
        super.setControllerPort(0)
    }

    protected var ctx: ConfigurableApplicationContext? = null

    override fun startSut(): String {
        ctx = SpringApplication.run(BaseGraphQLApplication::class.java, "--server.port=0")
        return "http://localhost:$sutPort"
    }

    val sutPort: Int
        get() = (ctx!!.environment
                .propertySources["server.ports"].source as Map<*, *>)["local.server.port"] as Int

    override fun isSutRunning(): Boolean {
        return ctx != null && ctx!!.isRunning
    }

    override fun stopSut() {
        ctx?.stop()
        ctx?.close()
    }


    override fun getPackagePrefixesToCover(): String {
        return "com.foo.graphql.base"
    }

    override fun getDbSpecifications(): MutableList<DbSpecification>? {
        return null
    }

    override fun getPreferredOutputFormat(): SutInfoDto.OutputFormat {
       return SutInfoDto.OutputFormat.KOTLIN_JUNIT_5
    }

    override fun resetStateOfSUT() {
    }

    override fun getInfoForAuthentication(): List<AuthenticationDto> {
        return listOf()
    }

    override fun getProblemInfo(): ProblemInfo {
        return GraphQlProblem("/graphql")
    }


}