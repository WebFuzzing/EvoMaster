package org.evomaster.core.database.extract.h2

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.client.java.controller.db.SqlScriptRunner
import org.evomaster.client.java.controller.internal.db.SchemaExtractor
import org.evomaster.core.database.DbActionTransformer
import org.evomaster.core.database.DbActionUtils
import org.evomaster.core.database.SqlInsertBuilder
import org.evomaster.core.search.gene.SqlPrimaryKeyGene
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable

class CatwatchSqlExtractTest : ExtractTestBaseH2(){

    override fun getSchemaLocation() = "/sql_schema/catwatch.sql"


    @Test
    fun testCreateAndExtract() {

        val schema = SchemaExtractor.extract(connection)

        assertNotNull(schema)

        assertAll(Executable { assertEquals("public", schema.name.toLowerCase()) },
                Executable { assertEquals(DatabaseType.H2, schema.databaseType) },
                Executable { assertEquals(5, schema.tables.size) },
                Executable { assertTrue(schema.tables.any { it.name == "CONTRIBUTOR" }) },
                Executable { assertTrue(schema.tables.any { it.name == "LANGUAGE_LIST" }) },
                Executable { assertTrue(schema.tables.any { it.name == "MAINTAINERS" }) },
                Executable { assertTrue(schema.tables.any { it.name == "PROJECT" }) },
                Executable { assertTrue(schema.tables.any { it.name == "STATISTICS" }) }
        )

        assertEquals(listOf("ID", "ORGANIZATION_ID", "SNAPSHOT_DATE"), schema.tables.filter { it.name == "CONTRIBUTOR" }.first().primaryKeySequence)
        assertEquals(listOf("ID", "SNAPSHOT_DATE"), schema.tables.filter { it.name == "STATISTICS" }.first().primaryKeySequence)
        assertEquals(listOf("ID"), schema.tables.filter { it.name == "PROJECT" }.first().primaryKeySequence)
        assertEquals(listOf<String>(), schema.tables.filter { it.name == "MAINTAINERS" }.first().primaryKeySequence)

    }

    @Test
    fun testDynamicFKsReferToAutoId(){

        val schema = SchemaExtractor.extract(connection)

        val builder = SqlInsertBuilder(schema)

        val execSqlIdMaps = mutableMapOf<Long, Long>()

        val insertions = builder.createSqlInsertionAction("lANGUAGE_LIST", setOf("PROJECT_ID"))

        DbActionUtils.randomizeDbActionGenes(insertions.toMutableList(), Randomness())

        assertEquals(2, insertions.size)
        assert(insertions[0].table.name.equals("PROJECT", ignoreCase = true))
        assert(insertions[1].table.name.equals("lANGUAGE_LIST", ignoreCase = true))

        val projectId = (insertions[0].seeGenes().filterIsInstance<SqlPrimaryKeyGene>()).first().uniqueId

        val dtoForPersons = DbActionTransformer.transform(listOf(insertions[0]), execSqlIdMaps)
        val responseForPersons = SqlScriptRunner.execInsert(connection, dtoForPersons.insertions)

        assertNotNull(responseForPersons)
        assert(responseForPersons.containsValue(projectId))

        execSqlIdMaps.putAll(responseForPersons)

        val dtoForDoctors = DbActionTransformer.transform(listOf(insertions[1]), execSqlIdMaps)
        SqlScriptRunner.execInsert(connection, dtoForDoctors.insertions)

        val selections = SqlScriptRunner.execCommand(connection, "SELECT * FROM lANGUAGE_LIST WHERE PROJECT_ID = ${execSqlIdMaps.getValue(projectId)};");
        assertEquals(1, selections.seeRows().size);
    }

}