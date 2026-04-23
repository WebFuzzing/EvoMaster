package com.foo.graphql.onlyerrors

import graphql.kickstart.tools.GraphQLQueryResolver
import org.springframework.stereotype.Component

@Component
open class QueryResolver : GraphQLQueryResolver {

    fun error(x : String) : Boolean{
        throw Exception("only error")
    }
}
