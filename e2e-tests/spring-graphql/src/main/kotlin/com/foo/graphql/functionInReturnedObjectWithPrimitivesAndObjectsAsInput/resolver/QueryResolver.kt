package com.foo.graphql.functionInReturnedObjectWithPrimitivesAndObjectsAsInput.resolver



import com.foo.graphql.functionInReturnedObjectWithPrimitivesAndObjectsAsInput.DataRepository
import com.foo.graphql.functionInReturnedObjectWithPrimitivesAndObjectsAsInput.type.Flower
import graphql.kickstart.tools.GraphQLQueryResolver
import org.springframework.stereotype.Component

@Component
open class QueryResolver(
        private val dataRepo: DataRepository
) : GraphQLQueryResolver {

    fun flowers(): Flower {
        return dataRepo.findFlower()
    }

}


