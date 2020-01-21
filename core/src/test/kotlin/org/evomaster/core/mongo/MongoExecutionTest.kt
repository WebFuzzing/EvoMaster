package org.evomaster.core.mongo

import org.evomaster.client.java.controller.api.dto.database.execution.MongoExecutionDto
import org.evomaster.client.java.controller.api.dto.database.execution.MongoOperationDto
import org.junit.jupiter.api.Test

class MongoExecutionTest {

    @Test
    fun testReadDto() {
        val dto = MongoExecutionDto()
        val mongoFindDto = MongoOperationDto()
        mongoFindDto.operationType = MongoOperationDto.Type.MONGO_FIND
        mongoFindDto.operationJsonStr = "{\"databaseName\":\"mydb\",\"collectionName\":\"mycollection\",\"query\":{}}\n"
        dto.mongoOperations.add(mongoFindDto)
        val mongoExecution = MongoExecution.fromDto(dto)
    }
}