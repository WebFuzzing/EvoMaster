package com.foo.graphql.base

import com.foo.graphql.SpringController


class BaseController : SpringController(GQLBaseApplication::class.java) {

    override fun schemaName() = GQLBaseApplication.SCHEMA_NAME

}