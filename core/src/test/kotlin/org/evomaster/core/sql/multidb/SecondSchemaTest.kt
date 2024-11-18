package org.evomaster.core.sql.multidb

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.client.java.sql.DbInfoExtractor
import org.junit.jupiter.api.Assertions.assertEquals
import java.sql.Connection

class SecondSchemaTest : MultiDbTestBase(){



    override fun verify(databaseType: DatabaseType,connection: Connection, name: String){

        val info = DbInfoExtractor.extract(connection)
        assertEquals(name.lowercase(), info.name.lowercase())
        assertEquals(2, info.tables.size)
        val foo = info.tables.find { it.name.lowercase() == "foo" }!!
        val bar = info.tables.find { it.name.lowercase() == "bar" }!!
        if(databaseType == DatabaseType.MYSQL){
            assertEquals(name.lowercase(), foo.schema.lowercase())
        } else {
            assertEquals("public", foo.schema.lowercase())
        }
        assertEquals("other", bar.schema.lowercase())
    }

    override fun getSchemaLocation() = "/sql_schema/multidb/secondschema.sql"
}