package com.foo.graphql.cycle

import com.foo.graphql.SpringController


class CycleController : SpringController(GQLCycleApplication::class.java) {

    override fun schemaName() = GQLCycleApplication.SCHEMA_NAME

}