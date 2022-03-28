package com.foo.graphql.nullableNonNullableInReturn


import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
open class GQLNullableNonNullableInReturnApplication{
    companion object{
        const val SCHEMA_NAME = "nullableNonNullableInReturn.graphqls"
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
    SpringApplication.run(GQLNullableNonNullableInReturnApplication::class.java,
            "--graphql.tools.schema-location-pattern=**/${GQLNullableNonNullableInReturnApplication.SCHEMA_NAME}")
}