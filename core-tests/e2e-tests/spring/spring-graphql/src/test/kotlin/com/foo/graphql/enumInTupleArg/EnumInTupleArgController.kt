package com.foo.graphql.enumInTupleArg

import com.foo.graphql.SpringController
import com.foo.graphql.enumIntupleArg.GQLEnumInTupleArgApplication

class EnumInTupleArgController : SpringController(GQLEnumInTupleArgApplication::class.java) {

    override fun schemaName() = GQLEnumInTupleArgApplication.SCHEMA_NAME

}
