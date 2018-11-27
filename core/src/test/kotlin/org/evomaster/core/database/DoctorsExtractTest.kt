package org.evomaster.core.database

import org.evomaster.client.java.controller.internal.db.SchemaExtractor
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class DoctorsExtractTest : ExtractTestBase() {

    override fun getSchemaLocation() = "/sql_schema/doctors.sql"


    @Test
    fun testIssueWithFK() {

        val schema = SchemaExtractor.extract(connection)

        val builder = SqlInsertBuilder(schema)

        val actions = builder.createSqlInsertionAction("DOCTORS", setOf("PERSON_ID"))

        val all = actions.flatMap { it.seeGenes() }.flatMap { it.flatView() }

        //force binding
        val randomness = Randomness()//.apply { updateSeed(1) }
        DbActionUtils.randomizeDbActionGenes(actions, randomness)

        val dto = DbActionTransformer.transform(actions)

        assertEquals(actions.size, dto.insertions.size)
    }


}