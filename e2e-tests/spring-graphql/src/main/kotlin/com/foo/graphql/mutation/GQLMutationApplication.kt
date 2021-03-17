package com.foo.graphql.mutation


import com.foo.graphql.cycle.GQLCycleApplication
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

/**
 * Created by asmab on 12-Mars-20.
 */
@SpringBootApplication
open class GQLMutationApplication{
    companion object{
        const val SCHEMA_NAME = "mutation.graphqls"
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
    SpringApplication.run(GQLMutationApplication::class.java,
            "--graphql.tools.schema-location-pattern=**/${GQLMutationApplication.SCHEMA_NAME}")
}