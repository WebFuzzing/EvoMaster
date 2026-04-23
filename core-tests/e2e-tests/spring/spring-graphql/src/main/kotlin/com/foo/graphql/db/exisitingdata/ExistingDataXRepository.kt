package com.foo.graphql.db.exisitingdata

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface ExistingDataXRepository : CrudRepository<ExistingDataX, Long>