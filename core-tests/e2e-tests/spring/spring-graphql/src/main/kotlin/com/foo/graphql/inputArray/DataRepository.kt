package com.foo.graphql.inputArray

import com.foo.graphql.inputArray.type.Flower
import com.foo.graphql.inputArray.type.Store
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicInteger


@Component
open class DataRepository {

    fun findFlower(store: Array<Store>): Flower? {
        //TODO check assertWithError
        //if (store[0].id == 0) return null else
            return Flower(4, 88)
    }

}




