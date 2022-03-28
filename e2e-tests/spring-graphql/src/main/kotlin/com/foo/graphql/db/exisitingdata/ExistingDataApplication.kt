package com.foo.graphql.db.exisitingdata

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
open class ExistingDataApplication

fun main(args: Array<String>) {
    SpringApplication.run(ExistingDataApplication::class.java,
        "--graphql.tools.schema-location-pattern=**/dbexisting.graphqls")
}