package com.foo.graphql.NullableNonNullableInReturn

import com.foo.graphql.SpringController
import com.foo.graphql.nullableNonNullableInReturn.GQLNullableNonNullableInReturnApplication

class NullableNonNullableInReturnController : SpringController(GQLNullableNonNullableInReturnApplication::class.java) {

    override fun schemaName() = GQLNullableNonNullableInReturnApplication.SCHEMA_NAME


}
