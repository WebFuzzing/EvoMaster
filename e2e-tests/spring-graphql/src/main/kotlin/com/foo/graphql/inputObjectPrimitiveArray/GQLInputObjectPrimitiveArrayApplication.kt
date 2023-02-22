package com.foo.graphql.inputObjectPrimitiveArray


import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication


@SpringBootApplication
open class GQLInputObjectPrimitiveArrayApplication{
    companion object{
        const val SCHEMA_NAME = "inputObjectPrimitiveArray.graphqls"
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
        GQLInputObjectPrimitiveArrayApplication::class.java,
            "--graphql.tools.schema-location-pattern=**/${GQLInputObjectPrimitiveArrayApplication.SCHEMA_NAME}")
}