package com.foo.graphql.db.base

import com.foo.graphql.db.SpringWithDbController


class DbBaseController : SpringWithDbController(DbBaseApplication::class.java) {

    override fun schemaName() = "dbbase.graphqls"

}