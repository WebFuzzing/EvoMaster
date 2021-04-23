package com.foo.graphql.db.base

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
open class DbBaseApplication

fun main(args: Array<String>) {
    SpringApplication.run(DbBaseApplication::class.java,
        "--graphql.tools.schema-location-pattern=**/dbbase.graphqls")
}