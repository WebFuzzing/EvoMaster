package org.evomaster.core.problem.rest.seeding.postman.pojos

import java.util.*

class Request {
    var method: String? = null
    var header: List<Header>? = null
    var url: Url? = null
    var body: Body? = null

    constructor() {
        header = ArrayList()
    }

    constructor(method: String?, header: List<Header>?, url: Url?, body: Body?) {
        this.method = method
        this.header = header
        this.url = url
        this.body = body
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val request = o as Request
        return method == request.method &&
                header == request.header &&
                url == request.url &&
                body == request.body
    }

    override fun hashCode(): Int {
        return Objects.hash(method, header, url, body)
    }
}