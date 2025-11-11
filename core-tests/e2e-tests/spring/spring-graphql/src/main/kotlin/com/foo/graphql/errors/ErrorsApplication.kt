package com.foo.graphql.errors

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication


@SpringBootApplication
open class ErrorsApplication

fun main(args: Array<String>) {
    SpringApplication.run(ErrorsApplication::class.java,
            "--graphql.tools.schema-location-pattern=**/errors.graphqls")
}