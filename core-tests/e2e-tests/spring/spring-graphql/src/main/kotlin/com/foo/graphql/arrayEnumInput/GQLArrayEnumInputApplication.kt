package com.foo.graphql.arrayEnumInput


import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

/**
 * Created by asmab on 30-Nov-22.
 */
@SpringBootApplication
open class GQLArrayEnumInputApplication{
    companion object{
        const val SCHEMA_NAME = "arrayEnumInput.graphqls"
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
    SpringApplication.run(GQLArrayEnumInputApplication::class.java,
            "--graphql.tools.schema-location-pattern=**/${GQLArrayEnumInputApplication.SCHEMA_NAME}")
}