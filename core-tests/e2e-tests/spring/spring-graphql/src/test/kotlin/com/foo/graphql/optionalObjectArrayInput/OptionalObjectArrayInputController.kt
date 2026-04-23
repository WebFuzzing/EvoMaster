package com.foo.graphql.optionalObjectArrayInput

import com.foo.graphql.SpringController



class OptionalObjectArrayInputController : SpringController(GQLOptionalObjectArrayInputApplication::class.java) {

    override fun schemaName() = GQLOptionalObjectArrayInputApplication.SCHEMA_NAME


}
