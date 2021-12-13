package com.foo.graphql.functionInReturnedObjectWithPrimitivesAndObjectsAsInput

import com.foo.graphql.SpringController


class FunctionInReturnedObjectWithPrimitivesAndObjectsAsInputController : SpringController(GQLFunctionInReturnedObjectsWithPrimitivesAndObjectsAsInput::class.java) {

    override fun schemaName() = GQLFunctionInReturnedObjectsWithPrimitivesAndObjectsAsInput.SCHEMA_NAME

}