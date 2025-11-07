package com.foo.graphql.unionWithInput

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

/**
 * Created by asmab on 29-Mars-20.
 */
@SpringBootApplication
open class GQLUnionWithInputApplication{
    companion object{
        const val SCHEMA_NAME = "unionWithInput.graphqls"
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
    SpringApplication.run(GQLUnionWithInputApplication::class.java,
            "--graphql.tools.schema-location-pattern=**/${GQLUnionWithInputApplication.SCHEMA_NAME}")
}