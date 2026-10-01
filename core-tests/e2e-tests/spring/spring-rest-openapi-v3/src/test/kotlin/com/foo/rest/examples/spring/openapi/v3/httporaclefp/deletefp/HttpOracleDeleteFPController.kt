package com.foo.rest.examples.spring.openapi.v3.httporaclefp.deletefp

import com.foo.rest.examples.spring.openapi.v3.SpringController


class HttpOracleDeleteFPController: SpringController(HttpOracleDeleteFPApplication::class.java){

    override fun resetStateOfSUT() {
        HttpOracleDeleteFPApplication.reset()
    }
}