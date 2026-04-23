package com.foo.graphql.cycleInTuple


import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

/**
 * Created by asmab on 12-Mars-23.
 */
@SpringBootApplication
open class GQLCycleInTupleApplication{
    companion object{
        const val SCHEMA_NAME = "cycleInTuple.graphqls"
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
        GQLCycleInTupleApplication::class.java,
            "--graphql.tools.schema-location-pattern=**/${GQLCycleInTupleApplication.SCHEMA_NAME}")
}