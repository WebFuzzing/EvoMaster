package com.foo.graphql.tupleWithOptLimitInLast


import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

/**
 * Created by asmab on 15-Fev-23.
 */
@SpringBootApplication
open class TupleWithOptLimitInLastApplication{
    companion object{
        const val SCHEMA_NAME = "tupleWithOptLimitInLast.graphqls"
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
        TupleWithOptLimitInLastApplication::class.java,
            "--graphql.tools.schema-location-pattern=**/${TupleWithOptLimitInLastApplication.SCHEMA_NAME}")
}