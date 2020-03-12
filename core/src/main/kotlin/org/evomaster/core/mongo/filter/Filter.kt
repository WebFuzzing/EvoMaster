package org.evomaster.core.mongo.filter

abstract class Filter {

    abstract fun <T,K> accept(visitor: MongoFilterVisitor<T,K>, arg: K): T;
}