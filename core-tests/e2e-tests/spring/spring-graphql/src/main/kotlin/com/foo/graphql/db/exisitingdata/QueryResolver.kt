package com.foo.graphql.db.exisitingdata

import graphql.kickstart.tools.GraphQLQueryResolver
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import javax.persistence.EntityManager

@Component
class QueryResolver(
    @Autowired private val manager: EntityManager) : GraphQLQueryResolver {

    fun getY() : List<ExistingDataY>{
        return manager.createQuery("select y from ExistingDataY y where y.x.id=42", ExistingDataY::class.java)
            .resultList
            .let { if(it.isNotEmpty()) it else listOf() }
    }

}

