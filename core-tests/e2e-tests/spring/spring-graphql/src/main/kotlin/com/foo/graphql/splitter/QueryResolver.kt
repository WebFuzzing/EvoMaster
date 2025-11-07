package com.foo.graphql.splitter

import graphql.kickstart.tools.GraphQLQueryResolver
import org.springframework.stereotype.Component

@Component
open class QueryResolver : GraphQLQueryResolver {

    fun isInt(x : String) : Boolean{
        //here would lead to some errors if x is not double
        val d = x.length.toDouble()
        if (d % 2 == 0.0){
            return true
        } else {
            throw Exception("error for splitting")
        }
    }
}
