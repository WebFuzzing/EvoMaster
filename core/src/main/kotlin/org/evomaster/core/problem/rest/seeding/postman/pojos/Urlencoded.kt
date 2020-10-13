package org.evomaster.core.problem.rest.seeding.postman.pojos

import java.util.*

class Urlencoded {
    var key: String? = null
    var value: String? = null
    var type: String? = null

    constructor() {}

    constructor(key: String?, value: String?, type: String?) {
        this.key = key
        this.value = value
        this.type = type
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val that = o as Urlencoded
        return key == that.key &&
                value == that.value &&
                type == that.type
    }

    override fun hashCode(): Int {
        return Objects.hash(key, value, type)
    }
}