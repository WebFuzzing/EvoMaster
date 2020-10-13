package org.evomaster.core.problem.rest.seeding.postman.pojos

import java.util.*

class Query {
    var key: String? = null
    var value: String? = null

    constructor() {}

    constructor(key: String?, value: String?) {
        this.key = key
        this.value = value
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val query = o as Query
        return key == query.key &&
                value == query.value
    }

    override fun hashCode(): Int {
        return Objects.hash(key, value)
    }
}