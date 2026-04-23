package com.foo.graphql.interfacesFunctions

import com.foo.graphql.SpringController



class InterfacesFunctionsController : SpringController(GQLInterfacesFunctionsApplication::class.java) {

    override fun schemaName() = GQLInterfacesFunctionsApplication.SCHEMA_NAME


}
