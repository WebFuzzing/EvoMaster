package com.foo.graphql.inputObject

import com.foo.graphql.SpringController



class InputObjectController : SpringController(GQLInputObjectApplication::class.java) {

    override fun schemaName() = GQLInputObjectApplication.SCHEMA_NAME


}
