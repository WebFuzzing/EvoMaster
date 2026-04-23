package com.foo.graphql.functionInReturnedObjectWithPrimitivesAndObjectsAsInput2.resolver



import com.foo.graphql.functionInReturnedObjectWithPrimitivesAndObjectsAsInput2.DataRepository
import com.foo.graphql.functionInReturnedObjectWithPrimitivesAndObjectsAsInput2.type.Flower
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


