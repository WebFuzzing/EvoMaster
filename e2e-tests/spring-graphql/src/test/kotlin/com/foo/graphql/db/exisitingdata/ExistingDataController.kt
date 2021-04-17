package com.foo.graphql.db.exisitingdata

import com.foo.graphql.SpringController


class ExistingDataController : SpringController(ExistingDataApplication::class.java) {

    override fun schemaName() = "dbexisiting.graphqls"

}