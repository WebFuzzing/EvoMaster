package com.foo.graphql.db.exisitingdata

import com.sun.istack.NotNull
import javax.persistence.*


@Entity
data class ExistingDataY (
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L,

    @NotNull @OneToOne
    var x: ExistingDataX = ExistingDataX()
)