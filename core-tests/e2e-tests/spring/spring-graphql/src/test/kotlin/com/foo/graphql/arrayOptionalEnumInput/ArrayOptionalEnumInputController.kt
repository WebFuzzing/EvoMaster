package com.foo.graphql.arrayOptionalEnumInput

import com.foo.graphql.SpringController

class ArrayOptionalEnumInputController : SpringController(GQLArrayOptionalEnumInputApplication::class.java) {

    override fun schemaName() = GQLArrayOptionalEnumInputApplication.SCHEMA_NAME

}