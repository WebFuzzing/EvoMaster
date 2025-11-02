package com.foo.graphql.enumInput


import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

/**
 * Created by asmab on 29-Mars-20.
 */
@SpringBootApplication
open class GQLEnumInputApplication{
    companion object{
        const val SCHEMA_NAME = "enumInput.graphqls"
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
    SpringApplication.run(GQLEnumInputApplication::class.java,
            "--graphql.tools.schema-location-pattern=**/${GQLEnumInputApplication.SCHEMA_NAME}")
}