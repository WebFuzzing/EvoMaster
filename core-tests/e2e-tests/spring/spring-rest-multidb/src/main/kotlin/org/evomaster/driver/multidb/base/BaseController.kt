package org.evomaster.driver.multidb.base

import com.foo.rest.examples.multidb.base.BaseApplication
import org.evomaster.driver.multidb.SpringController

class BaseController : SpringController(BaseApplication::class.java){

    override fun extraSpringStartOptions(): List<String> {
        return listOf(
            "--spring.datasource.schema=classpath:/sql/base.sql"
        )
    }
}