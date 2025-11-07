package com.foo.graphql.db.exisitingdata

import javax.validation.constraints.NotNull
import javax.persistence.*

@Entity
data class ExistingDataX(
    @Id
    var id: Long = 0L,

    @NotNull
    var name: String = ""
)