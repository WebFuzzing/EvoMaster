package com.foo.graphql.arrayEnumInput

import com.foo.graphql.SpringController

class ArrayEnumInputController : SpringController(GQLArrayEnumInputApplication::class.java) {

    override fun schemaName() = GQLArrayEnumInputApplication.SCHEMA_NAME

}