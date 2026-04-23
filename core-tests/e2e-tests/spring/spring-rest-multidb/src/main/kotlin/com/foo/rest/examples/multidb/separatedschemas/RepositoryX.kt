package com.foo.rest.examples.multidb.separatedschemas

import org.springframework.data.repository.CrudRepository

interface RepositoryX : CrudRepository<EntityX, String>