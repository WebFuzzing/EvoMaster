package com.foo.graphql.db.tree

import com.foo.graphql.db.SpringWithDbController


class DbTreeController : SpringWithDbController(DbTreeApplication::class.java) {

    override fun schemaName() = "dbtree.graphqls"

}