package org.evomaster.core.output.service

import org.evomaster.client.java.controller.SutHandler
import org.evomaster.client.java.controller.api.dto.database.operations.InsertionDto


class FakeController : SutHandler {
    override fun startSut(): String {
        return ""
    }

    override fun stopSut() {
    }

    override fun resetStateOfSUT() {
    }

    override fun execInsertionsIntoDatabase(insertions: MutableList<InsertionDto>?) {
    }
}