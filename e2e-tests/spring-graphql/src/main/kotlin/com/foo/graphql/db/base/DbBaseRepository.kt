package com.foo.graphql.db.base

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface DbBaseRepository : CrudRepository<DbBase, Long> {
    fun findByName(name : String) : List<DbBase>
}