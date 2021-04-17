package com.foo.graphql.db.directint

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
open class DbDirectIntApplication

fun main(args: Array<String>) {
    SpringApplication.run(DbDirectIntApplication::class.java,
        "--graphql.tools.schema-location-pattern=**/dbdirectInt.graphqls")
}