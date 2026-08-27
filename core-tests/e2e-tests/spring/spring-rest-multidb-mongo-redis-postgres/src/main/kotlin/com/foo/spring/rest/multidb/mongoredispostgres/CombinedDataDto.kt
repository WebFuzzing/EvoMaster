package com.foo.spring.rest.multidb.mongoredispostgres

data class CombinedDataDto(
    val postgresFound: Boolean,
    val mongoFound: Boolean,
    val redisFound: Boolean
)
