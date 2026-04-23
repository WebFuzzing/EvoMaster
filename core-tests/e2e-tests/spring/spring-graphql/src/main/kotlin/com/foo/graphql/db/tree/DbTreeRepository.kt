package com.foo.graphql.db.tree

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface DbTreeRepository : CrudRepository<DbTree, Long>