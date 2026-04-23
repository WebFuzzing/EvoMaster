package com.foo.graphql.nullableInput


import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication


@SpringBootApplication
open class GQLNullableInputApplication{
    companion object{
        const val SCHEMA_NAME = "nullable.input.graphqls"
    }
}


/*
    API accessible at
    http://localhost:8080/graphql

    UI accessible at
    http://localhost:8080/graphiql
    (note the "i" between graph and ql...)

    UI graph representation at
    http://localhost:8080/voyager
 */
fun main(args: Array<String>) {
    SpringApplication.run(
        GQLNullableInputApplication::class.java,
            "--graphql.tools.schema-location-pattern=**/${GQLNullableInputApplication.SCHEMA_NAME}")
}