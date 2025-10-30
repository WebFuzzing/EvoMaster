package com.foo.graphql.db.directint

import javax.persistence.*
import javax.validation.constraints.NotNull

@Entity
data class DbDirectInt(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L,

    @NotNull
    var x : Int = 0,

    @NotNull
    var y : Int = 0
)