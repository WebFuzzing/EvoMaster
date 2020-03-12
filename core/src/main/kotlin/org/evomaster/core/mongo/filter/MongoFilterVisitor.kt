package org.evomaster.core.mongo.filter

import org.bson.Document

abstract class MongoFilterVisitor<T, K>() {

    abstract fun visit(eqFilter: EqFilter<*>, argument: K): T;

}