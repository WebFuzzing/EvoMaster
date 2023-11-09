package org.evomaster.core.sql.extract.mysql

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.client.java.sql.SchemaExtractor
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CreateTableUniqueTest : ExtractTestBaseMySQL() {
    override fun getSchemaLocation(): String  = "/sql_schema/mysql_alter_table_unique.sql"


    @Test
    fun testCreateAndExtract() {

        val schema = SchemaExtractor.extract(connection)

        assertNotNull(schema)

        assertEquals("test", schema.name.toLowerCase())
        assertEquals(DatabaseType.MYSQL, schema.databaseType)

        val tableDto = schema.tables.find { it.name.equals("suppliers", ignoreCase = true)  }

        assertNotNull(tableDto)
        assertEquals(4, tableDto!!.columns.size)

        tableDto.columns.apply {
            val supplier_id = find { it.name == "supplier_id" }
            assertNotNull(supplier_id)
            assertTrue(supplier_id!!.autoIncrement)
            assertTrue(supplier_id.primaryKey)

            val name = find { it.name == "name" }
            assertNotNull(name)
            assertFalse(name!!.nullable)
            // need to discuss for handling e.g., CONSTRAINT uc_name_address UNIQUE (name , address)
            assertTrue(name.unique)

            val phone = find { it.name == "phone" }
            assertNotNull(phone)
            assertFalse(phone!!.nullable)
            assertTrue(phone.unique)

            val address = find { it.name == "address" }
            assertNotNull(address)
            assertFalse(address!!.nullable)
            // need to discuss for handling e.g., CONSTRAINT uc_name_address UNIQUE (name , address)
            assertTrue(address.unique)
        }
    }
}