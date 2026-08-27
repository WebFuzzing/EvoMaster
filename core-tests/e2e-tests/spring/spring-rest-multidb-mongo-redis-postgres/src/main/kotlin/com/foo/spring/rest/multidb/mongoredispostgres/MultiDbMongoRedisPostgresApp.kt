package com.foo.spring.rest.multidb.mongoredispostgres

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import springfox.documentation.swagger2.annotations.EnableSwagger2

@EnableSwagger2
@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
open class MultiDbMongoRedisPostgresApp : SwaggerConfiguration() {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(MultiDbMongoRedisPostgresApp::class.java, *args)
        }
    }
}
