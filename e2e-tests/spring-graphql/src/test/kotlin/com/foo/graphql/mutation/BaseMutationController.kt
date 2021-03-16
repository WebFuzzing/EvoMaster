package com.foo.graphql.mutation

import com.foo.graphql.SpringController



class BaseMutationController : SpringController(BaseMutationGQLApplication::class.java) {

    override fun schemaName() = "mutation.graphqls"


}
