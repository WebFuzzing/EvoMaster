package org.evomaster.client.java.controller.mongo.operations.synthetic

import org.bson.BsonType
import org.evomaster.client.java.controller.mongo.operations.QueryOperation

open class InvertedTypeOperation (open val fieldName: String, open val type: BsonType) : QueryOperation()