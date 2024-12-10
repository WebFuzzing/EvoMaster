package com.foo.graphql.unionWithObject

import com.foo.graphql.SpringController



class UnionWithObjectController : SpringController(GQLUnionWithObjectApplication::class.java) {

    override fun schemaName() = GQLUnionWithObjectApplication.SCHEMA_NAME


}
