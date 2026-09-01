package com.foo.spring.rest.polyglotpersistence

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import springfox.documentation.swagger2.annotations.EnableSwagger2

@EnableSwagger2
@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
open class PolyglotPersistenceApp : SwaggerConfiguration() {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(PolyglotPersistenceApp::class.java, *args)
        }
    }
}
