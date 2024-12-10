package com.foo.graphql.enumOptionalInput

import com.foo.graphql.SpringController

class EnumOptionalInputController : SpringController(GQLEnumOptionalInputApplication::class.java) {

    override fun schemaName() = GQLEnumOptionalInputApplication.SCHEMA_NAME

}