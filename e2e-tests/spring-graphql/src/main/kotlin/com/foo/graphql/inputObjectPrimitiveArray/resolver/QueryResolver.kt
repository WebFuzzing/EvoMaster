package com.foo.graphql.inputObjectPrimitiveArray.resolver


import com.foo.graphql.inputObjectPrimitiveArray.DataRepository
import com.foo.graphql.inputObjectPrimitiveArray.type.Flower
import com.foo.graphql.inputObjectPrimitiveArray.type.Store
import graphql.kickstart.tools.GraphQLQueryResolver
import org.springframework.stereotype.Component

@Component
open class QueryResolver(
        private val dataRepo: DataRepository
) : GraphQLQueryResolver {


    fun flowers(store: Store?): Flower?{
        return dataRepo.findFlower(store)
    }


}


