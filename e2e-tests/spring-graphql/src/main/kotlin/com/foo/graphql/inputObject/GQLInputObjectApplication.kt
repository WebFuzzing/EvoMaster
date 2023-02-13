package com.foo.graphql.inputObject


import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

/**
 * Created by asmab on 12-Mars-20.
 */
@SpringBootApplication
open class GQLInputObjectApplication{
    companion object{
        const val SCHEMA_NAME = "inputObject.graphqls"
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
        GQLInputObjectApplication::class.java,
            "--graphql.tools.schema-location-pattern=**/${GQLInputObjectApplication.SCHEMA_NAME}")
}