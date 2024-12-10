package com.foo.graphql.nullableInput.resolver


import com.foo.graphql.nullableInput.DataRepository
import com.foo.graphql.nullableInput.type.Flower
import graphql.kickstart.tools.GraphQLQueryResolver
import org.springframework.stereotype.Component

@Component
open class QueryResolver(
        private val dataRepo: DataRepository
) : GraphQLQueryResolver {


    fun flowersNullInNullOut(id: Array<Int?>?): Flower?{
        return dataRepo.findFlowersNullInNullOut(id)
    }

    //flowersNullIn(id: [Int]!): Flower
    fun flowersNullIn(id: Array<Int?>): Flower? {
        return dataRepo.findFlowersNullIn(id)
    }

    //flowersNullOut(id: [Int!]): Flower
    fun flowersNullOut(id: Array<Int>?): Flower? {
        return dataRepo.findFlowersNullOut(id)
    }

    // flowersNotNullInOut(id: [Int!]!): Flower
    fun flowersNotNullInOut(id: Array<Int>): Flower?{
        return dataRepo.findFlowersNotNullInOut(id)
    }

    //flowersScalarNullable(id: Boolean):Flower
    fun flowersScalarNullable(id: Boolean?): Flower?{
        return dataRepo.findFlowersScalarNullable(id)
    }

    //flowersScalarNotNullable(id: Boolean!):Flower
    fun flowersScalarNotNullable(id: Boolean): Flower?{
        return dataRepo.findFlowersScalarNotNullable(id)
    }
}


