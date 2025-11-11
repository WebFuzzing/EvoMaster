package com.foo.graphql.db.directint

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
open class DbDirectIntApplication{
    companion object{
        const val SCHEMA_NAME = "dbdirectInt.graphqls"
    }
}

fun main(args: Array<String>) {
    SpringApplication.run(DbDirectIntApplication::class.java,
        "--graphql.tools.schema-location-pattern=**/${DbDirectIntApplication.SCHEMA_NAME}")
}