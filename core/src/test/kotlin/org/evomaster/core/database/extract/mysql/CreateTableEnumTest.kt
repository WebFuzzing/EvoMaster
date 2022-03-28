package org.evomaster.core.database.extract.mysql

import org.evomaster.client.java.controller.internal.db.SchemaExtractor
import org.evomaster.core.database.SqlInsertBuilder
import org.evomaster.core.search.gene.EnumGene
import org.evomaster.core.search.gene.sql.SqlNullable
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CreateTableEnumTest : ExtractTestBaseMySQL() {

    override fun getSchemaLocation(): String = "/sql_schema/mysql_create_enum.sql"

    @Test
    fun testCreateAndExtract() {

        val schema = SchemaExtractor.extract(connection)

        assertNotNull(schema)

        val tableDto = schema.tables.find { it.name.equals("shirts", ignoreCase = true) }
        assertNotNull(tableDto)
        assertEquals(2, tableDto!!.columns.size)
        assertEquals(1, tableDto.tableCheckExpressions.size)

        tableDto.columns.apply {
            val size = find { it.name.equals("size",ignoreCase = true) }
            assertNotNull(size)
            assertEquals("ENUM", size!!.type)
        }

        assertEquals("size enum('x-small','small','medium','large','x-large')",tableDto.tableCheckExpressions[0].sqlCheckExpression.toLowerCase())

        val builder = SqlInsertBuilder(schema)
        val actions = builder.createSqlInsertionAction("shirts", setOf("name", "size"))

        val sizeGene = actions[0].seeGenes().find { it.name == "size" }
        assertTrue(sizeGene is SqlNullable)
        (sizeGene as SqlNullable).apply {
            assertTrue(gene is EnumGene<*>)
            assertEquals(5, (gene as EnumGene<*>).values.size)
            assertTrue((gene as EnumGene<*>).values.containsAll(listOf("x-small","small","medium","large","x-large")))
        }
    }
}