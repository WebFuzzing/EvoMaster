package com.foo.graphql.cycle

import com.foo.graphql.SpringController


class BaseCycleController : SpringController(BaseCycleGQLApplication::class.java) {

    override fun schemaName() = "cycle.graphqls"

}