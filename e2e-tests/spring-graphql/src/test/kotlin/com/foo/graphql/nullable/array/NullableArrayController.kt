package com.foo.graphql.nullable.array

import com.foo.graphql.SpringController



class NullableArrayController : SpringController(GQLNullableArrayApplication::class.java) {

    override fun schemaName() = GQLNullableArrayApplication.SCHEMA_NAME


}
