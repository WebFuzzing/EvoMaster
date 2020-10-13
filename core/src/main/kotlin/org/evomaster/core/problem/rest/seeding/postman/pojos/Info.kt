package org.evomaster.core.problem.rest.seeding.postman.pojos

import java.util.*

class Info {
    private var _postman_id: String? = null
    var name: String? = null
    var schema: String? = null

    constructor() {}

    constructor(_postman_id: String?, name: String?, schema: String?) {
        this._postman_id = _postman_id
        this.name = name
        this.schema = schema
    }

    fun get_postman_id(): String? {
        return _postman_id
    }

    fun set_postman_id(_postman_id: String?) {
        this._postman_id = _postman_id
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val info = o as Info
        return _postman_id == info._postman_id &&
                name == info.name &&
                schema == info.schema
    }

    override fun hashCode(): Int {
        return Objects.hash(_postman_id, name, schema)
    }
}