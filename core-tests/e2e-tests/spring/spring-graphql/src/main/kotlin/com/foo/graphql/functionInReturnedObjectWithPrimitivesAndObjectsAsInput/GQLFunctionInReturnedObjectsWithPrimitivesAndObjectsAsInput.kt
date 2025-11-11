package com.foo.graphql.functionInReturnedObjectWithPrimitivesAndObjectsAsInput


import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

/**
 * Created by asmab on 10-Dec-21.
 */
@SpringBootApplication
open class GQLFunctionInReturnedObjectsWithPrimitivesAndObjectsAsInput{
    companion object{
        const val SCHEMA_NAME = "functionInReturnedObjectWithPrimitivesAndObjectsAsInput.graphqls"
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
        GQLFunctionInReturnedObjectsWithPrimitivesAndObjectsAsInput::class.java,
            "--graphql.tools.schema-location-pattern=**/${GQLFunctionInReturnedObjectsWithPrimitivesAndObjectsAsInput.SCHEMA_NAME}")
}