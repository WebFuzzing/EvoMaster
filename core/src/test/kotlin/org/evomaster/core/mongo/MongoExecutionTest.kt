package org.evomaster.core.mongo

import org.evomaster.client.java.controller.api.dto.database.execution.MongoExecutionDto
import org.evomaster.client.java.controller.api.dto.database.execution.ExecutedFindOperationDto
import org.evomaster.client.java.controller.api.dto.database.execution.FindOperationDto
import org.evomaster.client.java.controller.api.dto.database.execution.FindResultDto
import org.evomaster.client.java.controller.mongo.ExecutedFindOperation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MongoExecutionTest {

    @Test
    fun testReadDto() {
        val findOperationDto = FindOperationDto()
        findOperationDto.databaseName = "mydb"
        findOperationDto.collectionName = "mycollection"
        findOperationDto.queryJsonStr = "{}"

        val findResultDto = FindResultDto()
        findResultDto.findResultType = FindResultDto.FindResultType.SUMMARY
        findResultDto.hasReturnedAnyDocument = true

        val executedFindDto = ExecutedFindOperationDto()
        executedFindDto.findOperationDto = findOperationDto
        executedFindDto.findResultDto = findResultDto

        val dto = MongoExecutionDto()
        dto.executedFindOperationDtos.add(executedFindDto)
        val mongoExecution = MongoExecution.fromDto(dto)
        assertEquals(1, mongoExecution.executedFindOperations.size)
        val executedFindOperation = mongoExecution.executedFindOperations[0]
        assertEquals("mydb", executedFindOperation.findOperation.databaseName)
        assertEquals("mycollection", executedFindOperation.findOperation.collectionName)
        assertEquals(0, executedFindOperation.findOperation.queryDocument.size)
    }
}