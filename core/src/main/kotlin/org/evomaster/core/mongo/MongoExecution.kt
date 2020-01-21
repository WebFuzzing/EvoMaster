package org.evomaster.core.mongo

import com.google.gson.Gson
import org.evomaster.client.java.controller.api.dto.database.execution.MongoExecutionDto
import org.evomaster.client.java.controller.api.dto.database.execution.MongoOperationDto
import org.evomaster.client.java.instrumentation.shared.mongo.MongoFindOperation
import org.evomaster.client.java.instrumentation.shared.mongo.MongoOperation


class MongoExecution {


    companion object {

        fun fromDto(dto: MongoExecutionDto?): MongoExecution {
            val mongoOperations = dto!!.mongoOperations.map { parseMongoOperation(it) }.toMutableList()
            return MongoExecution()
        }

        private fun parseMongoOperation(dto: MongoOperationDto): MongoOperation {
            when (dto.operationType) {
                MongoOperationDto.Type.MONGO_FIND -> {
                    val findOp = Gson().fromJson<MongoFindOperation>(dto.operationJsonStr!!, MongoFindOperation::class.java)
                    return findOp
                }
                else -> {
                    throw RuntimeException("")
                }
            }
        }
    }

}

