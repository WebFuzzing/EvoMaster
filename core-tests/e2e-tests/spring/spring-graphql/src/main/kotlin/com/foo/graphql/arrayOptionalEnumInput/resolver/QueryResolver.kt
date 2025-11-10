package com.foo.graphql.arrayOptionalEnumInput.resolver




import com.foo.graphql.arrayOptionalEnumInput.DataRepository
import com.foo.graphql.arrayOptionalEnumInput.type.Flower
import com.foo.graphql.arrayOptionalEnumInput.type.FlowerType
import graphql.kickstart.tools.GraphQLQueryResolver
import org.springframework.stereotype.Component
@Component
open class QueryResolver(
        private val dataRepo: DataRepository
) : GraphQLQueryResolver {
    fun flowersByType(type: Array<FlowerType?>?): Flower?{
        return dataRepo.findByFlTy(type)
    }

}






