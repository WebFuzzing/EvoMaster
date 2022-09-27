package com.foo.graphql.fieldWithDifferentArgument.resolver


import com.foo.graphql.fieldWithDifferentArgument.DataRepository
import com.foo.graphql.fieldWithDifferentArgument.type.Flower

import graphql.kickstart.tools.GraphQLQueryResolver
import org.springframework.stereotype.Component

@Component
open class QueryResolver(
        private val dataRepo: DataRepository
) : GraphQLQueryResolver {



    fun flower (id: Int, color: String) : Flower {
        return dataRepo.findByIdAndColor(id, color)
    }

   /* fun store (): Store {
        return
    }*/

}


