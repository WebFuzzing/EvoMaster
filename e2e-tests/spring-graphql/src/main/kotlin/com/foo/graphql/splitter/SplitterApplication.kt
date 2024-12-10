package com.foo.graphql.splitter

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication


@SpringBootApplication
open class SplitterApplication

fun main(args: Array<String>) {
    SpringApplication.run(SplitterApplication::class.java,
            "--graphql.tools.schema-location-pattern=**/splitter.graphqls")
}