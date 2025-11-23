package com.foo.rest.examples.spring.openapi.v3.security.xss.stored.html

import com.foo.rest.examples.spring.openapi.v3.SpringController

class XSSStoredController: SpringController(XSSStoredApplication::class.java){
    override fun resetStateOfSUT() {
        val app = ctx!!.getBean(XSSStoredApplication::class.java)
        app.resetDB()
    }
}