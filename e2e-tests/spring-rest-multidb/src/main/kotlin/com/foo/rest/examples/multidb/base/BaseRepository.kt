package com.foo.rest.examples.multidb.base

import org.springframework.data.repository.CrudRepository

interface BaseRepository : CrudRepository<BaseEntity, String>