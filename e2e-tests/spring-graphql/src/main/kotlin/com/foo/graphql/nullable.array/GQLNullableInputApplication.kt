package com.foo.graphql.nullable.array


import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication


@SpringBootApplication
open class GQLNullableArrayApplication{
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
        GQLNullableArrayApplication::class.java,
            "--graphql.tools.schema-location-pattern=**/${GQLNullableArrayApplication.SCHEMA_NAME}")
}