package com.foo.graphql.errors

import com.foo.graphql.SpringController


class ErrorsController : SpringController(ErrorsApplication::class.java) {

    override fun schemaName() = "errors.graphqls"

}