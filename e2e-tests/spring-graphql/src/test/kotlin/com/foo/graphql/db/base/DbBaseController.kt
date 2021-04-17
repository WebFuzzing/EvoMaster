package com.foo.graphql.db.base

import com.foo.graphql.SpringController


class DbBaseController : SpringController(DbBaseApplication::class.java) {

    override fun schemaName() = "dbbase.graphqls"

}