package com.foo.graphql.db.tree

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
open class DbTreeApplication

fun main(args: Array<String>) {
    SpringApplication.run(DbTreeApplication::class.java,
        "--graphql.tools.schema-location-pattern=**/dbtree.graphqls")
}