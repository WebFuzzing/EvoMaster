package com.foo.rest.examples.bb.httpslocalhost

import com.foo.rest.examples.bb.SpringController
import org.evomaster.client.java.controller.problem.ProblemInfo
import org.evomaster.client.java.controller.problem.RestProblem
import org.springframework.boot.SpringApplication

class BBHttpsLocalhostController : SpringController(BBHttpsLocalhostApplication::class.java) {

    override fun startSut(): String {
        ctx = SpringApplication.run(applicationClass,
            "--server.port=0",
            "--server.ssl.enabled=true",
            "--server.ssl.key-store=classpath:httpslocalhost/keystore.p12",
            "--server.ssl.key-store-password=password",
            "--server.ssl.key-store-type=PKCS12",
            "--server.ssl.key-alias=test"
        )
        return "https://localhost:$sutPort"
    }

    override fun getProblemInfo(): ProblemInfo {
        return RestProblem(
            "https://localhost:$sutPort/v3/api-docs",
            null
        )
    }
}
