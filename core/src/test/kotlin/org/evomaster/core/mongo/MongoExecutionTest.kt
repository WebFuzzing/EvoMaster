package org.evomaster.core.mongo

import org.evomaster.client.java.controller.api.dto.database.execution.MongoExecutionDto
import org.evomaster.client.java.controller.api.dto.database.execution.MongoOperationDto
import org.evomaster.client.java.instrumentation.shared.mongo.MongoFindOperation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
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
        assertEquals(1, mongoExecution.mongoOperations.size)
        assertTrue(mongoExecution.mongoOperations[0] is MongoFindOperation)
        val mongoFindOperation = mongoExecution.mongoOperations[0] as MongoFindOperation
        assertEquals("mydb", mongoFindOperation.databaseName)
        assertEquals("mycollection", mongoFindOperation.collectionName)
        assertEquals(0, mongoFindOperation.query.size)
    }
}