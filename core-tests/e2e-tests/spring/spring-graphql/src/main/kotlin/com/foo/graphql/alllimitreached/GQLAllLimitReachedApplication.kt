package com.foo.graphql.alllimitreached

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication


@SpringBootApplication
open class GQLAllLimitReachedApplication{
    companion object{
        const val SCHEMA_NAME = "allLimitReached.graphqls"
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
    SpringApplication.run(GQLAllLimitReachedApplication::class.java,
            "--graphql.tools.schema-location-pattern=**/${GQLAllLimitReachedApplication.SCHEMA_NAME}")
}