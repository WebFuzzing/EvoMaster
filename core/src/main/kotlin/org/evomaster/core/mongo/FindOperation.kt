package org.evomaster.core.mongo

import com.google.gson.Gson
import org.bson.Document
import org.evomaster.client.java.controller.api.dto.database.execution.FindOperationDto

class FindOperation(val databaseName: String,
                    val collectionName: String,
                    val queryDocument: Document) {

    companion object {

        fun fromDto(dto: FindOperationDto): FindOperation {
            return FindOperation(databaseName = dto.databaseName,
                    collectionName = dto.collectionName,
                    queryDocument = Gson().fromJson(dto.queryJsonStr, Document::class.java))
        }
    }

}