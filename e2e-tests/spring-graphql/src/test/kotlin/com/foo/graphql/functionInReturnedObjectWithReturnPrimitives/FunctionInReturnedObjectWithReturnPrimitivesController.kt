package com.foo.graphql.functionInReturnedObjectWithReturnPrimitives


import com.foo.graphql.SpringController


class FunctionInReturnedObjectWithReturnPrimitivesController : SpringController(GQLFunctionInReturnedObjectsWithReturnPrimitives::class.java) {

    override fun schemaName() = GQLFunctionInReturnedObjectsWithReturnPrimitives.SCHEMA_NAME

}