package com.foo.spring.rest.mysql.exisitingdata

import com.foo.spring.rest.mysql.SpringRestMySqlController
import org.evomaster.client.java.controller.db.SqlScriptRunner

class ExistingDataController : SpringRestMySqlController(ExistingDataApp::class.java) {

    override fun pathToFlywayFiles() = "classpath:/schema/existingdata"


    override fun resetStateOfSUT() {
        super.resetStateOfSUT()

        SqlScriptRunner.execScript(connection, "INSERT INTO X (id) VALUES (42)")
    }
}