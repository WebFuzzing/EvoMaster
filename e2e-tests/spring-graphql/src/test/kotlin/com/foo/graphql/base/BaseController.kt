package com.foo.graphql.base

import com.foo.graphql.SpringController


class BaseController : SpringController(BaseGraphQLApplication::class.java) {

    override fun schemaName() = "base.graphqls"

}