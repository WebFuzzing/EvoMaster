package com.foo.graphql.db.exisitingdata

import com.foo.graphql.db.SpringWithDbController


class ExistingDataController : SpringWithDbController(ExistingDataApplication::class.java) {

    override fun schemaName() = "dbexisiting.graphqls"

}