package org.evomaster.driver.multidb.separatedschemas

import com.foo.rest.examples.multidb.separatedschemas.SeparatedSchemasApplication
import org.evomaster.client.java.sql.SqlScriptRunner
import org.evomaster.driver.multidb.SpringController

class SeparatedSchemasController : SpringController(SeparatedSchemasApplication::class.java){


    override fun resetStateOfSUT() {
        SqlScriptRunner.execCommand(connectionIfExist, """
            CREATE SCHEMA IF NOT EXISTS foo;
            CREATE SCHEMA IF NOT EXISTS bar;
            CREATE TABLE IF NOT EXISTS foo.EntityX;
            CREATE TABLE IF NOT EXISTS bar.EntityY;
        """.trimIndent())

        super.resetStateOfSUT()
    }
}