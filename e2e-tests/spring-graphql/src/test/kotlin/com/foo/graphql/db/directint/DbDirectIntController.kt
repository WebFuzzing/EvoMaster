package com.foo.graphql.db.directint

import com.foo.graphql.SpringController


class DbDirectIntController : SpringController(DbDirectIntApplication::class.java) {

    override fun schemaName() = "dbdirectInt.graphqls"

}