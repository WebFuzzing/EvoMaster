package org.evomaster.core.output.service

import org.evomaster.client.java.controller.SutHandler
import org.evomaster.client.java.controller.api.dto.database.operations.InsertionDto
import org.evomaster.client.java.controller.api.dto.database.operations.InsertionResultsDto
import org.evomaster.client.java.controller.api.dto.database.operations.MongoInsertionDto
import org.evomaster.client.java.controller.api.dto.database.operations.MongoInsertionResultsDto
import org.evomaster.client.java.sql.DbSpecification


class FakeController : SutHandler {
    override fun startSut(): String {
        return ""
    }

    override fun stopSut() {
    }

    override fun resetStateOfSUT() {
    }

    override fun execInsertionsIntoDatabase(insertions: MutableList<InsertionDto>?, previous: Array<InsertionResultsDto>): InsertionResultsDto? {
        return null
    }

    override fun execInsertionsIntoMongoDatabase(insertions: MutableList<MongoInsertionDto>?): MongoInsertionResultsDto? {
        return null
    }

    override fun getDbSpecifications(): MutableList<DbSpecification>? {
        return null
    }
}