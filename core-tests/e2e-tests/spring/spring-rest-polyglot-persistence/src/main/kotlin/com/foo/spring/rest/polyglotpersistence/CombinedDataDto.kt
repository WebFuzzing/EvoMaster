package com.foo.spring.rest.polyglotpersistence

data class CombinedDataDto(
    val postgresFound: Boolean,
    val mongoFound: Boolean,
    val redisFound: Boolean
)
