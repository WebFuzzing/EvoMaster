package com.foo.graphql.mutation.resolver



import com.foo.graphql.mutation.DataRepository
import com.foo.graphql.mutation.type.Flower
import graphql.kickstart.tools.GraphQLMutationResolver
import org.springframework.stereotype.Component

@Component
class MutationResolver(
        private val dataRepo: DataRepository) : GraphQLMutationResolver {


    fun addFlower(name: String?, type: String? , color: String?, price: Int?): Flower {
        return dataRepo.saveFlower(name, type, color,price)

    }

}




