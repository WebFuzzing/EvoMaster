package com.foo.graphql.unionWithInput

import com.foo.graphql.SpringController



class UnionWithInputController : SpringController(GQLUnionWithInputApplication::class.java) {

    override fun schemaName() = GQLUnionWithInputApplication.SCHEMA_NAME


}
