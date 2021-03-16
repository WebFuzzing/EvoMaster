package com.foo.graphql.input

import com.foo.graphql.SpringController



class BaseInputController : SpringController(BaseInputGQLApplication::class.java) {

    override fun schemaName() = "input.graphqls"


}
