package com.foo.graphql.fieldWithDifferentArgument

import com.foo.graphql.SpringController


class FieldWithDifferentArgumentController : SpringController(GQLFieldWithDifferentArgumentApplication::class.java) {

    override fun schemaName() = GQLFieldWithDifferentArgumentApplication.SCHEMA_NAME

}