package com.foo.graphql.functionInReturnedObjectWithReturnHavingFunctionItself


import com.foo.graphql.SpringController


class FunctionInReturnedObjectWithReturnHavingFunctionItselfController : SpringController(GQLFunctionInReturnedObjectsWithReturnHavingFunctionItself::class.java) {

    override fun schemaName() = GQLFunctionInReturnedObjectsWithReturnHavingFunctionItself.SCHEMA_NAME

}