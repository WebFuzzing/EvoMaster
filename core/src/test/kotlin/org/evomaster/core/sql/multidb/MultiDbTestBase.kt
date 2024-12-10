package org.evomaster.core.sql.multidb

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.client.java.sql.SqlScriptRunner
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.sql.Connection

abstract class MultiDbTestBase {

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

    protected abstract fun verify(databaseType: DatabaseType, connection: Connection, name: String)

    protected abstract fun getSchemaLocation(): String
}