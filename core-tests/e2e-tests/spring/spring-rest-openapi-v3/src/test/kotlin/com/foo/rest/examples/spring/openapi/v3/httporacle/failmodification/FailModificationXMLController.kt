package com.foo.rest.examples.spring.openapi.v3.httporacle.failmodification

import com.foo.rest.examples.spring.openapi.v3.SpringController
import com.foo.rest.examples.spring.openapi.v3.httporacle.failmodification.xml.XmlFailModificationApplication


class FailModificationXMLController: SpringController(XmlFailModificationApplication::class.java){
    override fun resetStateOfSUT() {
        XmlFailModificationApplication.reset()
    }
}
