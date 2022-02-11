package com.foo.spring.rest.mysql.exisitingdata

import com.foo.spring.rest.mysql.SpringRestMySqlController
import org.evomaster.client.java.controller.internal.db.DbSpecification

class ExistingDataController : SpringRestMySqlController(ExistingDataApp::class.java) {

    override fun pathToFlywayFiles() = "classpath:/schema/existingdata"


    override fun resetStateOfSUT() {
        super.resetStateOfSUT()

//        SqlScriptRunner.execScript(dbConnection, "INSERT INTO X (id) VALUES (42)")
    }

    override fun getDbSpecification(): DbSpecification? {
        val spec = super.getDbSpecification()
        spec?.initSqlScript = "INSERT INTO X (id) VALUES (42);"
        return spec
    }
}