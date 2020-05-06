package org.evomaster.core.mongo

import com.fasterxml.jackson.databind.ObjectMapper
import org.bson.Document
import org.evomaster.client.java.controller.api.dto.mongo.FindOperationDto

class FindOperation(val databaseName: String,
                    val collectionName: String,
                    val queryDocument: Document) {

    companion object {

        fun fromDto(dto: FindOperationDto): FindOperation {
            val mapper = ObjectMapper();
            return FindOperation(databaseName = dto.databaseName,
                    collectionName = dto.collectionName,
                    queryDocument = mapper.readValue(dto.queryJsonStr, Document::class.java))
        }
    }

}