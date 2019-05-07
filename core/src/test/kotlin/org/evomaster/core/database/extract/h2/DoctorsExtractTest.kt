package org.evomaster.core.database.extract.h2

import org.evomaster.client.java.controller.internal.db.SchemaExtractor
import org.evomaster.core.database.DbActionTransformer
import org.evomaster.core.database.DbActionUtils
import org.evomaster.core.database.SqlInsertBuilder
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DoctorsExtractTest : ExtractTestBaseH2() {

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