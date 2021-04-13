package com.foo.graphql.db.tree

import graphql.kickstart.tools.GraphQLQueryResolver
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import javax.persistence.EntityManager

@Component
class QueryResolver(
    @Autowired private val manager: EntityManager) : GraphQLQueryResolver {

    fun dbTreeByParentId(id : Long) : List<DbTree>{
        return manager.createQuery("SELECT * FROM DbTree WHERE parent_id = $id", DbTree::class.java).resultList
    }

}

