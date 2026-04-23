package com.foo.graphql.enumOptionalInput.resolver




import com.foo.graphql.enumOptionalInput.DataRepository
import com.foo.graphql.enumOptionalInput.type.Flower
import com.foo.graphql.enumOptionalInput.type.FlowerType
import graphql.kickstart.tools.GraphQLQueryResolver
import org.springframework.stereotype.Component
@Component
open class QueryResolver(
        private val dataRepo: DataRepository
) : GraphQLQueryResolver {
    fun flowersByType(type: FlowerType?): Flower?{
        return dataRepo.findByFlTy(type)
    }

}






