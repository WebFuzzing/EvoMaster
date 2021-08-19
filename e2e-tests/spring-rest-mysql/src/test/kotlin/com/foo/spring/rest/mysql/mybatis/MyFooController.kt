package com.foo.spring.rest.mysql.mybatis

import com.foo.spring.rest.mysql.SpringRestMySqlController

class MyFooController : SpringRestMySqlController(MyFooApp::class.java) {
    override fun pathToFlywayFiles() = "classpath:/schema/myfoo"
}