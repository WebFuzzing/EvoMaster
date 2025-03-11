package com.foo.rest.examples.multidb.separatedschemas

import org.springframework.data.repository.CrudRepository

interface RepositoryY : CrudRepository<EntityY, String>