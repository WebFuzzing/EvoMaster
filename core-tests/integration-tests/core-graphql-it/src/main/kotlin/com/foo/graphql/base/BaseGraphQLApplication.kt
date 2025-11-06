package com.foo.graphql.base

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

/**
 * Created by arcuri82 on 18-Jul-17.
 */
@SpringBootApplication
open class BaseGraphQLApplication


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
    SpringApplication.run(BaseGraphQLApplication::class.java, *args)
}