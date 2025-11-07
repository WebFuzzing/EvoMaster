package com.foo.graphql.functionInReturnedObjectWithPrimitivesAndObjectsAsInput2


import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

/**
 * Created by asmab on 10-Dec-21.
 */
@SpringBootApplication
open class GQLFunctionInReturnedObjectsWithPrimitivesAndObjectsAsInput2{
    companion object{
        const val SCHEMA_NAME = "functionInReturnedObjectWithPrimitivesAndObjectsAsInput2.graphqls"
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
        GQLFunctionInReturnedObjectsWithPrimitivesAndObjectsAsInput2::class.java,
            "--graphql.tools.schema-location-pattern=**/${GQLFunctionInReturnedObjectsWithPrimitivesAndObjectsAsInput2.SCHEMA_NAME}")
}