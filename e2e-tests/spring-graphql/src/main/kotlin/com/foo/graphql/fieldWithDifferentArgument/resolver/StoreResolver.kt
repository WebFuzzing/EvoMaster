package com.foo.graphql.fieldWithDifferentArgument.resolver

import com.foo.graphql.fieldWithDifferentArgument.DataRepository
import com.foo.graphql.fieldWithDifferentArgument.type.Flower
import com.foo.graphql.fieldWithDifferentArgument.type.Store



import graphql.kickstart.tools.GraphQLResolver
import org.springframework.stereotype.Component

@Component
class StoreResolver(
        private val dataRepo: DataRepository
) : GraphQLResolver<Store> {

        fun getId(store: Store) = store.id
        fun getName(store: Store) = store.name


        fun flower(store: Store, id:Int): Flower {
                return dataRepo.findById(store.id)
        }


}


