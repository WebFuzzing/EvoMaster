package com.foo.graphql.nullableInput

import com.foo.graphql.SpringController


class NullableInputController : SpringController(GQLNullableInputApplication::class.java) {

    override fun schemaName() = GQLNullableInputApplication.SCHEMA_NAME


}
