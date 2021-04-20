package com.foo.graphql.primitiveType

import com.foo.graphql.SpringController


class PrimitiveTypeController : SpringController(GQLPrimitiveTypeApplication::class.java) {

    override fun schemaName() = GQLPrimitiveTypeApplication.SCHEMA_NAME


}

