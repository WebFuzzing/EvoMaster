package org.evomaster.core.problem.rest.seeding.postman.pojos

import java.util.*

class Body {
    var mode: String? = null
    var raw: String? = null

    var urlencoded: List<Urlencoded>? = null

    constructor() {
        urlencoded = ArrayList()
    }

    constructor(mode: String?, raw: String?, urlencoded: List<Urlencoded>?) {
        this.mode = mode
        this.raw = raw
        this.urlencoded = urlencoded
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val body = o as Body
        return mode == body.mode &&
                raw == body.raw &&
                urlencoded == body.urlencoded
    }

    override fun hashCode(): Int {
        return Objects.hash(mode, raw, urlencoded)
    }
}