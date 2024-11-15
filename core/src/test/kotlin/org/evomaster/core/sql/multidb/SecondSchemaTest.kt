package org.evomaster.core.sql.multidb

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.client.java.sql.DbInfoExtractor
import org.evomaster.client.java.sql.SqlScriptRunner
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.sql.Connection

class SecondSchemaTest {

    @ParameterizedTest
    @EnumSource(names = ["MYSQL","POSTGRES","H2"])
    fun test(databaseType: DatabaseType){
        val name = "dbtest"
        val sqlSchemaCommand = this::class.java.getResource(getSchemaLocation()).readText()

        MultiDbUtils.startDatabase(databaseType)
        try {
            MultiDbUtils.resetDatabase(name, databaseType)
            val connection = MultiDbUtils.createConnection(name, databaseType)
            connection.use {
                SqlScriptRunner.execCommand(it, sqlSchemaCommand)
                verify(databaseType,it, name)
            }
        }finally {
            MultiDbUtils.stopDatabase(databaseType)
        }
    }

    private fun verify(databaseType: DatabaseType,connection: Connection, name: String){

        val info = DbInfoExtractor.extract(connection)
        assertEquals(name.lowercase(), info.name.lowercase())
        assertEquals(2, info.tables.size)
        val foo = info.tables.find { it.name.lowercase() == "foo" }!!
        val bar = info.tables.find { it.name.lowercase() == "bar" }!!
        if(databaseType == DatabaseType.MYSQL){
            assertEquals(name.lowercase(), foo.openGroupName.lowercase())
        } else {
            assertEquals("public", foo.openGroupName.lowercase())
        }
        assertEquals("other", bar.openGroupName.lowercase())
    }

    fun getSchemaLocation() = "/sql_schema/multidb/secondschema.sql"
}