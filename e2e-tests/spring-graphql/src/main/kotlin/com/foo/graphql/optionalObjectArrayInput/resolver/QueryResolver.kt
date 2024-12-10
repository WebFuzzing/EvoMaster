package com.foo.graphql.optionalObjectArrayInput.resolver



import com.foo.graphql.optionalObjectArrayInput.DataRepository
import com.foo.graphql.optionalObjectArrayInput.type.Flower
import com.foo.graphql.optionalObjectArrayInput.type.Store
import graphql.kickstart.tools.GraphQLQueryResolver
import org.springframework.stereotype.Component

@Component
open class QueryResolver(
        private val dataRepo: DataRepository
) : GraphQLQueryResolver {


    fun flowers(store: Array<Store?>?): Flower?{///opt array in and out following the GQL schema

        return dataRepo.findFlower(store)
    }

}


