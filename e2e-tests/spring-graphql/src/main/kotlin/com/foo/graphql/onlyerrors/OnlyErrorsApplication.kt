package com.foo.graphql.onlyerrors

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication


@SpringBootApplication
open class OnlyErrorsApplication

fun main(args: Array<String>) {
    SpringApplication.run(OnlyErrorsApplication::class.java,
            "--graphql.tools.schema-location-pattern=**/onlyerrors.graphqls")
}