package com.foo.graphql.base

import com.foo.graphql.SpringController
import org.springframework.boot.SpringApplication

class BaseWithOutschemaController: SpringController(GQLBaseApplication::class.java) {
    override fun schemaName() = GQLBaseApplication.SCHEMA_NAME

    override fun startSut(): String {
        ctx = SpringApplication.run(applicationClass,
            "--server.port=0",
            "--graphql.tools.schema-location-pattern=**/${schemaName()}",
            "--graphql.tools.introspection-enabled=false"

        )
        return "http://localhost:$sutPort"
    }




}