package com.foo.graphql.bb.base

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

/**
 * Created by arcuri82 on 18-Jul-17.
 */
@SpringBootApplication
open class GQLBaseApplication{
    companion object{
        const val SCHEMA_NAME = "base.graphqls"
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
    SpringApplication.run(GQLBaseApplication::class.java,
            "--graphql.tools.schema-location-pattern=**/${GQLBaseApplication.SCHEMA_NAME}")
}