package org.evomaster.core.problem.rest.seeding.postman.pojos

import java.util.*

class Url {
    var raw: String? = null
    var protocol: String? = null
    var host: List<String>? = null

    var path: List<String>? = null

    var query: List<Query>? = null

    constructor() {
        host = ArrayList()
        path = ArrayList()
        query = ArrayList()
    }

    constructor(raw: String?, protocol: String?, host: List<String>?, path: List<String>?, query: List<Query>?) {
        this.raw = raw
        this.protocol = protocol
        this.host = host
        this.path = path
        this.query = query
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val url = o as Url
        return raw == url.raw &&
                protocol == url.protocol &&
                host == url.host &&
                path == url.path &&
                query == url.query
    }

    override fun hashCode(): Int {
        return Objects.hash(raw, protocol, host, path, query)
    }
}