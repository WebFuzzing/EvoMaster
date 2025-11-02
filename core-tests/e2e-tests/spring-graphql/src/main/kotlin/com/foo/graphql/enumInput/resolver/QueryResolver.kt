package com.foo.graphql.enumInput.resolver



import com.foo.graphql.enumInput.DataRepository
import com.foo.graphql.enumInput.type.Flower
import com.foo.graphql.enumInput.type.FlowerType
import graphql.kickstart.tools.GraphQLQueryResolver
import org.springframework.stereotype.Component
@Component
open class QueryResolver(
        private val dataRepo: DataRepository
) : GraphQLQueryResolver {
    fun flowersByType(type: FlowerType): Flower?{
        return dataRepo.findByFlTy(type)
    }

}






