package org.evomaster.client.java.controller.mongo.operations

import org.bson.BsonType

open class TypeOperation (open val fieldName: String, open val type: BsonType) : QueryOperation()