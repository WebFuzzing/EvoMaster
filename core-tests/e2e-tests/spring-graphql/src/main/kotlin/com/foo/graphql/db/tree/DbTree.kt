package com.foo.graphql.db.tree

import javax.persistence.*


@Entity
data class DbTree (
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L,

    @ManyToOne
    var parent: DbTree? = null
)