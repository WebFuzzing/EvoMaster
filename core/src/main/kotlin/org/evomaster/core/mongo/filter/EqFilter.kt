package org.evomaster.core.mongo.filter

class EqFilter<V>(
        val fieldName: String,
        val value: V
) : Filter() {


    override fun <T, K> accept(visitor: MongoFilterVisitor<T, K>, argument: K): T {
        return visitor.visit(this, argument)
    }
}