package com.foo.rest.examples.spring.openapi.v3.httporaclefp.partialupdateputfp

import com.foo.rest.examples.spring.openapi.v3.SpringController


class HttpPartialUpdatePutFPController: SpringController(PartialUpdatePutFPApplication::class.java){

    override fun resetStateOfSUT() {
        PartialUpdatePutFPApplication.reset()
    }
}