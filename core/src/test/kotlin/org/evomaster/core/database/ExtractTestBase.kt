package org.evomaster.core.database

import org.evomaster.client.java.controller.db.SqlScriptRunner
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import java.sql.Connection
import java.sql.DriverManager


abstract class ExtractTestBase {

    companion object {

        @JvmStatic
        protected lateinit var connection: Connection

        private var sqlSchemaCommand : String? = null

        @BeforeAll
        @JvmStatic
        fun initClass() {
            connection = DriverManager.getConnection("jdbc:h2:mem:db_test", "sa", "")
        }

        @AfterAll
        @JvmStatic
        fun clean(){
            sqlSchemaCommand = null
            connection.close()
        }
    }


    @BeforeEach
    fun initTest() {

        //custom H2 command
        SqlScriptRunner.execCommand(connection, "DROP ALL OBJECTS;")

        if(sqlSchemaCommand == null){
            sqlSchemaCommand = this::class.java.getResource(getSchemaLocation()).readText()
        }

        SqlScriptRunner.execCommand(connection, sqlSchemaCommand)
    }

    abstract fun getSchemaLocation() : String
}