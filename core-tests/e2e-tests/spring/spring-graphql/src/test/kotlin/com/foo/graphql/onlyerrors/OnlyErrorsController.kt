package com.foo.graphql.onlyerrors

import com.foo.graphql.SpringController


class OnlyErrorsController : SpringController(OnlyErrorsApplication::class.java) {

    override fun schemaName() = "onlyerrors.graphqls"

}