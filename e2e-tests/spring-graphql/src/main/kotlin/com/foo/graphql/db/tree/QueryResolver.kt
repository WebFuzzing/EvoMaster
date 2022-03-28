package com.foo.graphql.db.tree

import graphql.kickstart.tools.GraphQLQueryResolver
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import javax.persistence.EntityManager

@Component
class QueryResolver(
    @Autowired private val manager: EntityManager,
    @Autowired private val repository: DbTreeRepository
    ) : GraphQLQueryResolver {

    companion object{
        const val NOT_FOUND = "NOT_FOUND"
        const val NO_PARENT = "NO_PARENT"
        const val WITH_PARENT = "WITH_PARENT"
    }

    fun dbTreeByParentId(id : Long) : String{
        val node = repository.findById(id).orElse(null)

        if (node == null){
            return NOT_FOUND
        }

        val query = manager.createNativeQuery("SELECT * FROM db_tree WHERE parent_id = $id")
        val list = query.resultList
        /*
            java Object withParent = list.isEmpty() ? null : list.get(0);
            is it equaillent with
         */
        val parent = if (list.isEmpty()) null else list[0]
        if (parent == null){
            return NO_PARENT
        }
        return WITH_PARENT
    }

}

