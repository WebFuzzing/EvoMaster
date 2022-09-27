package com.foo.graphql.fieldWithDifferentArgument

import com.foo.graphql.fieldWithDifferentArgument.type.Flower
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicInteger


@Component
open class DataRepository {

    private val flowers = mutableMapOf<Int?, Flower>()

    private val counter = AtomicInteger(3)

    init {
        listOf(
            Flower(0,  "Roses", "Red"),
                Flower(1,  "Tulips", "Pink"),
                Flower(2,  "Lilies", "White"),
                Flower(3,  "Limonium", "Purple")
        ).forEach { flowers[it.id] = it }

    }


   fun findByIdAndColor (id: Int, color: String): Flower {

       for(flower in flowers){
           if (flower.key == id) if (flower.value.color== color)
               return flower.value
       }
       return Flower(id,  "X", color)
   }



}




