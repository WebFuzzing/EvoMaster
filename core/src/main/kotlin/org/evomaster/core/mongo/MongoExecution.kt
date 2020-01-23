package org.evomaster.core.mongo

import com.google.gson.Gson
import org.evomaster.client.java.controller.api.dto.database.execution.MongoExecutionDto
import org.evomaster.client.java.controller.api.dto.database.execution.MongoOperationDto
import org.evomaster.client.java.instrumentation.shared.mongo.MongoFindOperation
import org.evomaster.client.java.instrumentation.shared.mongo.MongoOperation


class MongoExecution(val mongoOperations: MutableList<MongoOperation> = mutableListOf()) {

    companion object {

        fun fromDto(dto: MongoExecutionDto?): MongoExecution {
            val mongoExecution = MongoExecution()
            mongoExecution.mongoOperations += dto!!.mongoOperations.map { parseMongoOperation(it) }.toMutableList()
            return mongoExecution
        }

        private fun parseMongoOperation(dto: MongoOperationDto): MongoOperation {
            when (dto.operationType) {
                MongoOperationDto.Type.MONGO_FIND -> {
                    return Gson().fromJson(dto.operationJsonStr!!, MongoFindOperation::class.java)
                }
                else -> {
                    throw RuntimeException("")
                }
            }
        }
    }

}

