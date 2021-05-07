package com.foo.graphql.mutation

import com.foo.graphql.SpringController



class MutationController : SpringController(GQLMutationApplication::class.java) {

    override fun schemaName() = GQLMutationApplication.SCHEMA_NAME


}
