package com.foo.rest.examples.spring.openapi.v3.httporacle.invalidlocation.verbselection

import com.foo.rest.examples.spring.openapi.v3.SpringController
import com.foo.rest.examples.spring.openapi.v3.httporacle.invalidlocation.verbselection.HttpInvalidLocationVerbSelectionApplication


class HttpInvalidLocationVerbSelectionController: SpringController(HttpInvalidLocationVerbSelectionApplication::class.java){

    override fun resetStateOfSUT() {
        HttpInvalidLocationVerbSelectionApplication.reset()
    }
}
