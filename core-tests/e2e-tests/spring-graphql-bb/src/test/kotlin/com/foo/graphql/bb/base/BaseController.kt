package com.foo.graphql.bb.base

import com.foo.graphql.bb.SpringController


class BaseController : SpringController(GQLBaseApplication::class.java) {

    override fun schemaName() = GQLBaseApplication.SCHEMA_NAME

}