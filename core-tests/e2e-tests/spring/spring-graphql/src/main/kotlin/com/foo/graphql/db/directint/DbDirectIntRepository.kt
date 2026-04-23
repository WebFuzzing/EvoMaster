package com.foo.graphql.db.directint

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface DbDirectIntRepository : CrudRepository<DbDirectInt, Long>{
    fun findByXAndY(x : Int, y : Int) : List<DbDirectInt>
}