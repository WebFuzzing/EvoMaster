package com.foo.graphql.nullable.array

import com.foo.graphql.SpringController
import com.foo.graphql.nullable.GQLNullableInputApplication


class NullableInputController : SpringController(GQLNullableInputApplication::class.java) {

    override fun schemaName() = GQLNullableInputApplication.SCHEMA_NAME


}
