package com.foo.graphql.unionFunctions

import com.foo.graphql.SpringController


class UnionFunctionsController : SpringController(GQLUnionFunctionsApplication::class.java) {

    override fun schemaName() = GQLUnionFunctionsApplication.SCHEMA_NAME


}
