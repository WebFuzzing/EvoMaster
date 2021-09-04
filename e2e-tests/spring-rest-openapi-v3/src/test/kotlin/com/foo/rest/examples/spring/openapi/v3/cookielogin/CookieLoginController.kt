package com.foo.rest.examples.spring.openapi.v3.cookielogin

import com.foo.rest.examples.spring.openapi.v3.SpringController
import com.foo.rest.examples.spring.openapi.v3.fakecookieLogin.CookieLoginCenterApplication
import org.evomaster.client.java.controller.api.dto.AuthenticationDto
import org.evomaster.client.java.controller.api.dto.CookieLoginDto
import org.evomaster.client.java.controller.problem.ProblemInfo
import org.evomaster.client.java.controller.problem.RestProblem
import org.springframework.boot.SpringApplication
import org.springframework.context.ConfigurableApplicationContext

class CookieLoginController: SpringController(CookieLoginApplication::class.java){

    var ctx2: ConfigurableApplicationContext? = null

    override fun getProblemInfo(): ProblemInfo {
        return RestProblem(
                "http://localhost:$sutPort/v3/api-docs",
                listOf("/api/logintoken/login")
        )
    }

    override fun startSut(): String {
        ctx2 = SpringApplication.run(CookieLoginCenterApplication::class.java, "--server.port=0")
        return super.startSut()
    }

    override fun stopSut() {
        super.stopSut()
        ctx2?.stop()
        ctx2?.close()
    }

    private fun getFakeApplicationURL() : String{
        val port = (ctx2!!.environment.propertySources["server.ports"].source as Map<*, *>)["local.server.port"] as Int
        return "http://localhost:$port"
    }

    override fun getInfoForAuthentication(): List<AuthenticationDto> {
        return listOf(
                AuthenticationDto("Foo").apply { cookieLogin = CookieLoginDto().apply {
                    username = "foo"
                    usernameField = "username"
                    password = "123"
                    passwordField = "password"
                    loginEndpointUrl = "/api/logintoken/login"
                    httpVerb = CookieLoginDto.HttpVerb.POST
                    contentType = CookieLoginDto.ContentType.JSON
                } },
                AuthenticationDto("Bar").apply { cookieLogin = CookieLoginDto().apply {
                    username = "bar"
                    usernameField = "username"
                    password = "456"
                    passwordField = "password"
                    loginEndpointUrl = getFakeApplicationURL()+"/api/manager/login"
                    httpVerb = CookieLoginDto.HttpVerb.POST
                    contentType = CookieLoginDto.ContentType.JSON
                } }
        )
    }
}