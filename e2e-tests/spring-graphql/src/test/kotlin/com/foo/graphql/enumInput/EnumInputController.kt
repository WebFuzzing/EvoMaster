package com.foo.graphql.enumInput

import com.foo.graphql.SpringController

class EnumInputController : SpringController(GQLEnumInputApplication::class.java) {

    override fun schemaName() = GQLEnumInputApplication.SCHEMA_NAME

}