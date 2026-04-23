package org.evomaster.core.problem.webfrontend.service

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
open class HttpStaticServer{

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(HttpStaticServer::class.java, *args)
        }
    }

}