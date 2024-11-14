package org.evomaster.core.sql.multidb

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.client.java.sql.SchemaExtractor
import org.evomaster.client.java.sql.SqlScriptRunner
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.sql.Connection

class SecondSchemaTest {

    //TODO parameterize
    @Test
    fun test(){
        val databaseType = DatabaseType.H2
        val name = "dbtest"
        val sqlSchemaCommand = this::class.java.getResource(getSchemaLocation()).readText()

        MultiDbUtils.startDatabase(databaseType)
        try {
            val connection = MultiDbUtils.createConnection(name, databaseType)
            connection.use {
                MultiDbUtils.resetDatabase(databaseType, it)
                SqlScriptRunner.execCommand(it, sqlSchemaCommand)
                verify(it, name)
            }
        }finally {
            MultiDbUtils.stopDatabase(databaseType)
        }
    }

    private fun verify(connection: Connection, name: String){

        val info = SchemaExtractor.extract(connection)
        assertEquals(name.lowercase(), info.name.lowercase())
        assertEquals(2, info.tables.size)
        //TODO other checks
    }

    fun getSchemaLocation() = "/sql_schema/multidb/secondschema.sql"
}