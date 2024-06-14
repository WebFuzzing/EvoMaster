package com.foo.base

import org.evomaster.client.java.controller.ExternalSutController
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto
import org.evomaster.client.java.controller.api.dto.SutInfoDto
import org.evomaster.client.java.sql.DbSpecification
import org.evomaster.client.java.controller.problem.ProblemInfo
import org.evomaster.client.java.controller.problem.RestProblem


class BaseExternalDriver : ExternalSutController(){

    //not great to fix a port... but no other option :(
    //trying to setup an ephemeral one here would be a mess
    var sutPort = 43125

    override fun getJVMParameters(): Array<String> {
        return emptyArray()
    }

    override fun getPackagePrefixesToCover(): String {
        return "com.foo.base"
    }

    override fun getMaxAwaitForInitializationInSeconds(): Long {
        return 90
    }

    override fun getDbSpecifications(): MutableList<DbSpecification>? {
        return null
    }

    override fun getPreferredOutputFormat(): SutInfoDto.OutputFormat {
        return SutInfoDto.OutputFormat.KOTLIN_JUNIT_5
    }

    override fun resetStateOfSUT() {
    }

    override fun getProblemInfo(): ProblemInfo {
        return  RestProblem(getBaseURL() + "/v3/api-docs", null)
    }



    override fun getPathToExecutableJar(): String {
        return "target/base.jar"
    }

    override fun getInputParameters(): Array<String> {
        return arrayOf("--server.port=$sutPort")
    }

    override fun postStop() {
    }

    override fun getLogMessageOfInitializedServer(): String {
        return "Started BaseApplication"
    }

    override fun preStart() {
    }

    override fun postStart() {
    }

    override fun getBaseURL(): String {
        return "http://localhost:$sutPort"
    }

    override fun getInfoForAuthentication(): List<AuthenticationDto> {
        return listOf()
    }

    override fun preStop() {
    }
}