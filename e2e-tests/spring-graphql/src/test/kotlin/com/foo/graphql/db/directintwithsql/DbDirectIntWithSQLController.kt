package com.foo.graphql.db.directintwithsql

import com.foo.graphql.db.SpringWithDbController
import com.foo.graphql.db.directint.DbDirectIntApplication


class DbDirectIntWithSQLController : SpringWithDbController(DbDirectIntApplication::class.java) {

    override fun schemaName() = "dbdirectIntQuery.graphqls"

}