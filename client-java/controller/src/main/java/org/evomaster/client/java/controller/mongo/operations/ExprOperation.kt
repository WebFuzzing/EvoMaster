package org.evomaster.client.java.controller.mongo.operations

import org.bson.BsonType

open class ExprOperation (open val fieldName: String, open val type: BsonType) : QueryOperation()