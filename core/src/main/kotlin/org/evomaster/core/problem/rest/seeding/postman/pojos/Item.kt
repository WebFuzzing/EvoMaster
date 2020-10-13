package org.evomaster.core.problem.rest.seeding.postman.pojos

import java.util.*

class Item {
    var name: String? = null
    var request: Request? = null
    var response: List<Any>? = null

    constructor() {
        response = ArrayList()
    }

    constructor(name: String?, request: Request?, response: List<Any>?) {
        this.name = name
        this.request = request
        this.response = response
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val item = o as Item
        return name == item.name &&
                request == item.request &&
                response == item.response
    }

    override fun hashCode(): Int {
        return Objects.hash(name, request, response)
    }
}