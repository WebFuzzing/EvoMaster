package com.foo.graphql.nullableNonNullableInInput

import com.foo.graphql.SpringController

class NullableNonNullableInInputController : SpringController(GQLNullableNonNullableInInputApplication::class.java) {

    override fun schemaName() = GQLNullableNonNullableInInputApplication.SCHEMA_NAME


}
