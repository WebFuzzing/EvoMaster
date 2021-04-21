package com.foo.graphql.db.base

import javax.persistence.*

@Entity
data class DbBase(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L,
    var name: String? = null
)