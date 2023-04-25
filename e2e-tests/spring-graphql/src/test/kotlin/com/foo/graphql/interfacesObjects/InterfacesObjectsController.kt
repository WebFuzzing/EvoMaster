package com.foo.graphql.interfacesObjects

import com.foo.graphql.SpringController


class InterfacesObjectsController : SpringController(GQLInterfacesObjectsApplication::class.java) {

    override fun schemaName() = GQLInterfacesObjectsApplication.SCHEMA_NAME


}
