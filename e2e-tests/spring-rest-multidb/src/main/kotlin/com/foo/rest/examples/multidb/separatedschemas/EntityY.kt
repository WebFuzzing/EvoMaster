package com.foo.rest.examples.multidb.separatedschemas

import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table
import javax.validation.constraints.NotNull

@Entity
@Table(name = "EntityY", schema = "bar")
open class EntityY {

    @get:Id
    @get:NotNull
    open var id: String? = null
}