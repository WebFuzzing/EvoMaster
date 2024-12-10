package com.foo.graphql.mutationObject

import com.foo.graphql.SpringController


class MutationObjectController : SpringController(GQLMutationObjectApplication::class.java) {

    override fun schemaName() = GQLMutationObjectApplication.SCHEMA_NAME


}
