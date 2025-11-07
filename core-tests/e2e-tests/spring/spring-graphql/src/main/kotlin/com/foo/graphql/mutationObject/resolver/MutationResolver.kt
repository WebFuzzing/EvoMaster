package com.foo.graphql.mutationObject.resolver



import com.foo.graphql.mutationObject.DataRepository
import com.foo.graphql.mutationObject.type.Flower
import com.foo.graphql.mutationObject.type.Price
import graphql.kickstart.tools.GraphQLMutationResolver
import org.springframework.stereotype.Component

@Component
class MutationResolver(
        private val dataRepo: DataRepository
) : GraphQLMutationResolver {


    fun addFlower( price: Price?): Flower? {
        return dataRepo.saveFlower(price)

    }

}




