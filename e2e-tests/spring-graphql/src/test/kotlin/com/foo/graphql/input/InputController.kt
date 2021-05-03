package com.foo.graphql.input

import com.foo.graphql.SpringController



class InputController : SpringController(GQLInputApplication::class.java) {

    override fun schemaName() = GQLInputApplication.SCHEMA_NAME


}
