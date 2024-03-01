package org.evomaster.core.sql.extract.h2

import org.evomaster.client.java.sql.SchemaExtractor
import org.evomaster.core.sql.SqlActionTransformer
import org.evomaster.core.sql.SqlActionUtils
import org.evomaster.core.sql.SqlInsertBuilder
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.problem.enterprise.SampleType
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

        val ind = RestIndividual(mutableListOf(), SampleType.RANDOM,null,actions.toMutableList(),null,-1)

        //force binding
        val randomness = Randomness()
        SqlActionUtils.randomizeDbActionGenes(actions, randomness)

        val dto = SqlActionTransformer.transform(actions)

        assertEquals(actions.size, dto.insertions.size)
    }


}
