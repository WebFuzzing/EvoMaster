package com.foo.spring.rest.multidb.mongoredispostgres

import javax.persistence.Id

class MongoPerson {
    @Id
    var id: String? = null
    var age: Int? = null

    constructor()

    constructor(age: Int) {
        this.age = age
    }
}
