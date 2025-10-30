package com.foo.graphql.db.directint

import com.foo.graphql.db.SpringWithDbController


class DbDirectIntController : SpringWithDbController(DbDirectIntApplication::class.java) {

    override fun schemaName() = "dbdirectInt.graphqls"

}