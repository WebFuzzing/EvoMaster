package com.foo.graphql.nullableNonNullableInReturn

import com.foo.graphql.SpringController

class NullableNonNullableInReturnController : SpringController(GQLNullableNonNullableInReturnApplication::class.java) {

    override fun schemaName() = GQLNullableNonNullableInReturnApplication.SCHEMA_NAME


}
