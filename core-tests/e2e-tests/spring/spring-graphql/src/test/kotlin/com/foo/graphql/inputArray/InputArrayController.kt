package com.foo.graphql.inputArray

import com.foo.graphql.SpringController



class InputArrayController : SpringController(GQLInputArrayApplication::class.java) {

    override fun schemaName() = GQLInputArrayApplication.SCHEMA_NAME


}
