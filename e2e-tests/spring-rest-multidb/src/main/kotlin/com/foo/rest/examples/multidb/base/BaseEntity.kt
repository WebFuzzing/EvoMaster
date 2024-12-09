package com.foo.rest.examples.multidb.base

import javax.persistence.Entity
import javax.persistence.Id
import javax.validation.constraints.NotNull

@Entity
open class BaseEntity(

    @get:Id @get:NotNull
    open var id: String? = null,

    @get:NotNull
    open var name: String? = null,
)
