package com.foo.graphql.primitiveType


import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

/**
 * Created by asmab on 26-Mars-20.
 */
@SpringBootApplication
open class GQLPrimitiveTypeApplication{
    companion object{
        const val SCHEMA_NAME = "primitiveType.graphqls"
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
    SpringApplication.run(GQLPrimitiveTypeApplication::class.java,
            "--graphql.tools.schema-location-pattern=**/${GQLPrimitiveTypeApplication.SCHEMA_NAME}")
}