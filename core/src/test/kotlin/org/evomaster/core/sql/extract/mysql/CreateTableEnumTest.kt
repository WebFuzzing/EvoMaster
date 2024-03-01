package org.evomaster.core.sql.extract.mysql

import org.evomaster.client.java.sql.SchemaExtractor
import org.evomaster.core.sql.SqlInsertBuilder
import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.optional.NullableGene
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

        val sizeGene = actions[0].seeTopGenes().find { it.name == "size" }
        assertTrue(sizeGene is NullableGene)
        (sizeGene as NullableGene).apply {
            assertTrue(gene is EnumGene<*>)
            assertEquals(5, (gene as EnumGene<*>).values.size)
            assertTrue((gene as EnumGene<*>).values.containsAll(listOf("x-small","small","medium","large","x-large")))
        }
    }
}