package com.foo.graphql.functionInReturnedObjectWithReturnHavingFunctionItself


import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

/**
 * Created by asmab on 10-Dec-21.
 */
@SpringBootApplication
open class GQLFunctionInReturnedObjectsWithReturnHavingFunctionItself{
    companion object{
        const val SCHEMA_NAME = "functionInReturnedObjectWithReturnHavingFunctionItself.graphqls"
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
        GQLFunctionInReturnedObjectsWithReturnHavingFunctionItself::class.java,
            "--graphql.tools.schema-location-pattern=**/${GQLFunctionInReturnedObjectsWithReturnHavingFunctionItself.SCHEMA_NAME}")
}