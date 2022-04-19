package org.evomaster.core.database.extract.mysql

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.client.java.controller.internal.db.SchemaExtractor
import org.evomaster.core.database.SqlInsertBuilder
import org.evomaster.core.search.gene.DoubleGene
import org.evomaster.core.search.gene.FloatGene
import org.evomaster.core.search.gene.IntegerGene
import org.evomaster.core.search.gene.LongGene
import org.evomaster.core.search.gene.sql.SqlNullable
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CreateTableBoundedNumberTest : ExtractTestBaseMySQL() {

    override fun getSchemaLocation(): String = "/sql_schema/mysql_create_numberdatetypes.sql"

    @Test
    fun testCreateAndExtract() {

        val schema = SchemaExtractor.extract(connection)

        assertNotNull(schema)

        assertEquals("test", schema.name)
        assertEquals(DatabaseType.MYSQL, schema.databaseType)
        assertTrue(schema.tables.any { it.name.equals("BoundedNumberTable", ignoreCase = true) })

        val columns = schema.tables.first { it.name.equals("BoundedNumberTable", ignoreCase = true) }.columns

        columns.apply {
            assertEquals(8, size)
            assertEquals(listOf("bc", "dd", "dc1", "dc2", "dc3", "dc4", "tc1","tc2"), map { it.name })
            assertEquals(listOf(-1, -1, 2, 3, 1, 1, -1, -1), map { it.scale })
            assertTrue(this[0].isUnsigned)
            assertEquals(10, this[2].size)
            assertFalse(this[2].isUnsigned)
            assertEquals(5, this[3].size)
            assertFalse(this[3].isUnsigned)
            assertEquals(2, this[4].size)
            assertFalse(this[4].isUnsigned)
            assertEquals(2, this[5].size)
            assertTrue(this[5].isUnsigned)
            assertTrue(this[7].isUnsigned)
        }

        val builder = SqlInsertBuilder(schema)

        val actions = builder.createSqlInsertionAction("BoundedNumberTable", setOf("*"))

        actions[0].seeGenes().apply {
            assertEquals(8, size)
            assertEquals(listOf("bc", "dd", "dc1", "dc2", "dc3", "dc4", "tc1", "tc2"), map { it.name })

            val bc = this[0]
            assertTrue(bc is LongGene)
            (bc as LongGene).apply {
                assertNotNull(this.min)
                assertEquals(0L, this.min)
            }

            val dd = this[1]
            assertTrue(dd is SqlNullable)
            (dd as SqlNullable).apply {
                assertTrue(this.gene is DoubleGene)
                assertNull((this.gene as DoubleGene).scale)
            }


            val dc1 = this[2]
            assertTrue(dc1 is FloatGene)
            (dc1 as FloatGene).apply {
                assertNotNull(scale)
                assertEquals(2, scale)
                assertEquals(-99999999.99f, min)
                assertEquals(99999999.99f, max)
            }

            val dc2 = this[3]
            assertTrue(dc2 is FloatGene)
            (dc2 as FloatGene).apply {
                assertNotNull(scale)
                assertEquals(3, scale)
                assertEquals(-99.999f, min)
                assertEquals(99.999f, max)
            }

            val dc3 = this[4]
            assertTrue(dc3 is FloatGene)
            (dc3 as FloatGene).apply {
                assertNotNull(scale)
                assertEquals(1, scale)
                assertEquals(-9.9f, min)
                assertEquals(9.9f, max)
            }

            val dc4 = this[5]
            assertTrue(dc4 is FloatGene)
            (dc4 as FloatGene).apply {
                assertNotNull(scale)
                assertEquals(1, scale)
                assertEquals(0.0f, min)
                assertEquals(9.9f, max)
            }

            val tc1 = this[6]
            assertTrue(tc1 is IntegerGene)
            (tc1 as IntegerGene).apply {
                assertEquals(Byte.MIN_VALUE.toInt(), min)
                assertEquals(Byte.MAX_VALUE.toInt(), max)
            }


            val tc2 = this[7]
            assertTrue(tc2 is IntegerGene)
            (tc2 as IntegerGene).apply {
                assertEquals(0, min)
                assertEquals(255, max)
            }


        }
    }
}