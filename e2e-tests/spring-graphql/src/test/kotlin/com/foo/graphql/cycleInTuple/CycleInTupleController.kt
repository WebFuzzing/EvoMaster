package com.foo.graphql.cycleInTuple

import com.foo.graphql.SpringController


class CycleInTupleController : SpringController(GQLCycleInTupleApplication::class.java) {

    override fun schemaName() = GQLCycleInTupleApplication.SCHEMA_NAME

}