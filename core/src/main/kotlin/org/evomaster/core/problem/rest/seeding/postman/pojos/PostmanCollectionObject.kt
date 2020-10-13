package org.evomaster.core.problem.rest.seeding.postman.pojos

import java.util.*

class PostmanCollectionObject {
    var info: Info? = null
    var item: List<Item>? = null

    constructor() {
        item = ArrayList()
    }

    constructor(info: Info?, item: List<Item>?) {
        this.info = info
        this.item = item
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val that = o as PostmanCollectionObject
        return info == that.info &&
                item == that.item
    }

    override fun hashCode(): Int {
        return Objects.hash(info, item)
    }
}