package com.foo.spring.rest.mysql.exisitingdata

import com.foo.spring.rest.mysql.SpringRestMySqlController
import org.evomaster.client.java.sql.DbSpecification

class ExistingDataController : SpringRestMySqlController(ExistingDataApp::class.java) {

    override fun pathToFlywayFiles() = "classpath:/schema/existingdata"


    override fun resetStateOfSUT() {
        super.resetStateOfSUT()

//        SqlScriptRunner.execScript(dbConnection, "INSERT INTO X (id) VALUES (42)")
    }

    override fun getDbSpecifications(): MutableList<DbSpecification>? {
        val spec = super.getDbSpecifications()
        if (spec != null && spec.isNotEmpty())
            return mutableListOf(spec[0].withInitSqlScript("INSERT INTO X (id) VALUES (42);"))
        return spec
    }
}