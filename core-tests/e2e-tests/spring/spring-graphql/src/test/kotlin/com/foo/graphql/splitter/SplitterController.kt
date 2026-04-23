package com.foo.graphql.splitter

import com.foo.graphql.SpringController


class SplitterController : SpringController(SplitterApplication::class.java) {

    override fun schemaName() = "errors.graphqls"

}