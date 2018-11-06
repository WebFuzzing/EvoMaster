package org.evomaster.core.database

import org.evomaster.clientJava.controller.db.SqlScriptRunner
import org.evomaster.clientJava.controller.internal.db.SchemaExtractor
import org.evomaster.clientJava.controllerApi.dto.database.schema.DatabaseType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable
import java.sql.Connection
import java.sql.DriverManager

class AlterTableConstraintTest {


    companion object {

        private lateinit var connection: Connection

        @BeforeAll
        @JvmStatic
        fun initClass() {
            connection = DriverManager.getConnection("jdbc:h2:mem:db_test", "sa", "")
        }
    }

    @BeforeEach
    fun initTest() {

        //custom H2 command
        SqlScriptRunner.execCommand(connection, "DROP ALL OBJECTS;")
    }


    @Test
    fun testCreateAndExtract() {

        val sqlCommand = this::class.java.getResource("/sql_schema/passports.sql").readText()

        SqlScriptRunner.execCommand(connection, sqlCommand)

        val schema = SchemaExtractor.extract(connection)

        assertNotNull(schema)

        assertAll(Executable { assertEquals("public", schema.name.toLowerCase()) },
                Executable { assertEquals(DatabaseType.H2, schema.databaseType) },
                Executable { assertEquals(2, schema.tables.size) },
                Executable { assertTrue(schema.tables.any { it.name == "COUNTRIES" }, "missing table COUNTRIES") },
                Executable { assertTrue(schema.tables.any { it.name == "PASSPORTS" }, "missing table PASSPORTS") }
        )


        assertEquals(true, schema.tables.filter { it.name == "PASSPORTS" }.first().columns.filter { it.name == "COUNTRY_ID"}.first().unique)
        assertEquals(true, schema.tables.filter { it.name == "PASSPORTS" }.first().columns.filter { it.name == "PASSPORT_NUMBER"}.first().unique)

        assertEquals(1, schema.tables.filter { it.name == "PASSPORTS" }.first().columns.filter { it.name == "PASSPORT_NUMBER"}.first().lowerBound)
        assertNull(schema.tables.filter { it.name == "PASSPORTS" }.first().columns.filter { it.name == "PASSPORT_NUMBER"}.first().upperBound)

    }


}