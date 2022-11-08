package com.foo.graphql.nullableInput

import com.foo.graphql.nullableInput.type.Flower
import org.springframework.stereotype.Component


@Component
open class DataRepository {

    //flowersNullInNullOut(id: [Int]): Flower
    fun findFlowersNullInNullOut(id: Array<Int?>?): Flower? {
        return if (id == null) null
        else
            if (id.all { it != null }) Flower(0, "flowerNameX") else null
    }

    //flowersNullIn(id: [Int]!): Flower
    fun findFlowersNullIn(id: Array<Int?>): Flower? {
        return if (id.all { it != null }) Flower(0, "flowerNameX") else null
    }

    //flowersNullOut(id: [Int!]): Flower
    fun findFlowersNullOut(id: Array<Int>?): Flower? {
        return if (id == null) null
        else Flower(0, "flowerNameX")
    }

    // flowersNotNullInOut(id: [Int!]!): Flower
    fun findFlowersNotNullInOut(id: Array<Int>): Flower? {
        return Flower(0, "flowerNameX")
    }

    fun findFlowersScalarNullable(id: Boolean?): Flower? {
        return if (id == null) null else
            if (id) Flower(1, "flowerNameIdTrue")
        else Flower(0, "flowerNameIdFalse")
    }

    fun findFlowersScalarNotNullable(id: Boolean): Flower? {
        return if (id) Flower(1, "flowerNameIdTrue")
        else Flower(0, "flowerNameIdFalse")
    }
}




