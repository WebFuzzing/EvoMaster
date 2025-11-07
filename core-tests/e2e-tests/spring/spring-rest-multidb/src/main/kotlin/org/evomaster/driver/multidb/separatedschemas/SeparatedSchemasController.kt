package org.evomaster.driver.multidb.separatedschemas

import com.foo.rest.examples.multidb.separatedschemas.SeparatedSchemasApplication
import org.evomaster.client.java.sql.SqlScriptRunner
import org.evomaster.driver.multidb.SpringController

class SeparatedSchemasController : SpringController(SeparatedSchemasApplication::class.java){

    override fun extraSpringStartOptions(): List<String> {
        return listOf(
            "--spring.datasource.schema=classpath:/sql/separatedschemas.sql"
        )
    }

}