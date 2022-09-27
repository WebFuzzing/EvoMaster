package com.foo.graphql.fieldWithDifferentArgument.resolver


import com.foo.graphql.fieldWithDifferentArgument.DataRepository
import com.foo.graphql.fieldWithDifferentArgument.type.Flower

import graphql.kickstart.tools.GraphQLQueryResolver
import org.springframework.stereotype.Component

@Component
open class StoreResolver(
        private val dataRepo: DataRepository
) : GraphQLQueryResolver {


}


