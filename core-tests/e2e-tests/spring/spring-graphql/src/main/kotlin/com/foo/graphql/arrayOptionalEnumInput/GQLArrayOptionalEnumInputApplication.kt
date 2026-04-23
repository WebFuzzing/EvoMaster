package com.foo.graphql.arrayOptionalEnumInput


import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

/**
 * Created by asmab on 30-Nov-22.
 */
@SpringBootApplication
open class GQLArrayOptionalEnumInputApplication{
    companion object{
        const val SCHEMA_NAME = "arrayOptionalEnumInput.graphqls"
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
        GQLArrayOptionalEnumInputApplication::class.java,
            "--graphql.tools.schema-location-pattern=**/${GQLArrayOptionalEnumInputApplication.SCHEMA_NAME}")
}