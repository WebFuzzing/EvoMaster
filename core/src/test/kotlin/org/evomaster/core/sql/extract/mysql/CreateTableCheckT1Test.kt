package org.evomaster.core.sql.extract.mysql

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.client.java.sql.SchemaExtractor
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CreateTableCheckT1Test : ExtractTestBaseMySQL() {
    override fun getSchemaLocation(): String  = "/sql_schema/mysql_create_t1.sql"


    @Test
    fun testCreateAndExtract() {

        val schema = SchemaExtractor.extract(connection)

        assertNotNull(schema)

        assertEquals("test", schema.name)
        assertEquals(DatabaseType.MYSQL, schema.databaseType)
        assertTrue(schema.tables.any { it.name.equals("t1", ignoreCase = true) })
        assertEquals(3, schema.tables.first { it.name.equals("t1", ignoreCase = true) }.columns.size)

        assertEquals(listOf("c1", "c2", "c3"),schema.tables.first { it.name.equals("t1", ignoreCase = true) }.columns.map { it.name })

        schema.tables.first { it.name.equals("t1", ignoreCase = true) }.tableCheckExpressions.apply {
            assertEquals(6, size)
            /*
                https://dev.mysql.com/doc/refman/8.0/en/create-table-check-constraints.html
                CONSTRAINT `c1_nonzero` CHECK ((`c1` <> 0)),
                CONSTRAINT `c2_positive` CHECK ((`c2` > 0)),
                CONSTRAINT `t1_chk_1` CHECK ((`c1` <> `c2`)),
                CONSTRAINT `t1_chk_2` CHECK ((`c1` > 10)),
                CONSTRAINT `t1_chk_3` CHECK ((`c3` < 100)),
                CONSTRAINT `t1_chk_4` CHECK ((`c1` > `c3`))
             */
            assertTrue(any { it.sqlCheckExpression == "(c1 <> 0)" })
            assertTrue(any { it.sqlCheckExpression == "(c2 > 0)" })
            assertTrue(any { it.sqlCheckExpression == "(c1 <> c2)" })
            assertTrue(any { it.sqlCheckExpression == "(c1 > 10)" })
            assertTrue(any { it.sqlCheckExpression == "(c3 < 100)" })
            assertTrue(any { it.sqlCheckExpression == "(c1 > c3)" })
        }
    }
}