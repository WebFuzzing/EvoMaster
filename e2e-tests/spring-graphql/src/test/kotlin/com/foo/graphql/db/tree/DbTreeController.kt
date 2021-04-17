package com.foo.graphql.db.tree

import com.foo.graphql.SpringController


class DbTreeController : SpringController(DbTreeApplication::class.java) {

    override fun schemaName() = "dbtree.graphqls"

}