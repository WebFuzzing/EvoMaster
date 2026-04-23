package com.foo.graphql.errors

import graphql.kickstart.tools.GraphQLQueryResolver
import org.springframework.stereotype.Component

@Component
open class QueryResolver : GraphQLQueryResolver {

    fun isInt(x : String) : Boolean{
        //here would lead to some errors if x is not double
        val d = x.toDouble()
        return d % 1 == 0.0
    }
}
