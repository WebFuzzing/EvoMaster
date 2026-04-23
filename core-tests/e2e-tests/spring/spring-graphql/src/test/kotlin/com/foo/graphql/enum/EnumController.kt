package com.foo.graphql.enum

import com.foo.graphql.SpringController

class EnumController : SpringController(GQLEnumApplication::class.java) {

    override fun schemaName() = GQLEnumApplication.SCHEMA_NAME

}
