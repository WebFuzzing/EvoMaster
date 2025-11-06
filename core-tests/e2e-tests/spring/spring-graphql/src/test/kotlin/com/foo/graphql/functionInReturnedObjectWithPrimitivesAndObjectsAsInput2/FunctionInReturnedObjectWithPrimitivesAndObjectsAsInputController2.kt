package com.foo.graphql.functionInReturnedObjectWithPrimitivesAndObjectsAsInput2

import com.foo.graphql.SpringController


class FunctionInReturnedObjectWithPrimitivesAndObjectsAsInputController2 : SpringController(GQLFunctionInReturnedObjectsWithPrimitivesAndObjectsAsInput2::class.java) {

    override fun schemaName() = GQLFunctionInReturnedObjectsWithPrimitivesAndObjectsAsInput2.SCHEMA_NAME

}