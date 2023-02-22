package com.foo.graphql.inputObjectPrimitiveArray

import com.foo.graphql.inputObjectPrimitiveArray.type.Flower
import com.foo.graphql.inputObjectPrimitiveArray.type.Store
import org.springframework.stereotype.Component


@Component
open class DataRepository {

    fun findFlower(store: Store?): Flower? {

       // if (store == null) return Flower(0, 0)
       // else
            if (store?.name == null) return Flower(0, 0) else
            if (store.name!!.isEmpty()) return Flower(0, 0)
            else if (store.name!!.any { it == null }) return Flower(0, 0)
        return Flower(1, 10)


    }

}




