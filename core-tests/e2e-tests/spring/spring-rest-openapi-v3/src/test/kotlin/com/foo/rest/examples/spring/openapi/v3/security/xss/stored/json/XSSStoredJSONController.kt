package com.foo.rest.examples.spring.openapi.v3.security.xss.stored.json

import com.foo.rest.examples.spring.openapi.v3.SpringController

class XSSStoredJSONController: SpringController(XSSStoredJSONApplication::class.java){
    override fun resetStateOfSUT() {
        val app = ctx!!.getBean(XSSStoredJSONApplication::class.java)
        app.resetDB()
    }
}