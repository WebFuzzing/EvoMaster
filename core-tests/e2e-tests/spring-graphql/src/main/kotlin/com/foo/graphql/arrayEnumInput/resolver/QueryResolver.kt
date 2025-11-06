package com.foo.graphql.arrayEnumInput.resolver




import com.foo.graphql.arrayEnumInput.DataRepository
import com.foo.graphql.arrayEnumInput.type.Flower
import com.foo.graphql.arrayEnumInput.type.FlowerType
import graphql.kickstart.tools.GraphQLQueryResolver
import org.springframework.stereotype.Component
@Component
open class QueryResolver(
        private val dataRepo: DataRepository
) : GraphQLQueryResolver {
    fun flowersByType(type: Array<FlowerType>): Flower?{
        return dataRepo.findByFlTy(type)
    }

}






