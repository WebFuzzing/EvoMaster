package com.foo.spring.rest.multidb.mongoredispostgres

import javax.persistence.Id

class MongoPerson {
    @Id
    var id: String? = null
    var name: String? = null

    constructor()

    constructor(name: String) {
        this.name = name
    }
}
