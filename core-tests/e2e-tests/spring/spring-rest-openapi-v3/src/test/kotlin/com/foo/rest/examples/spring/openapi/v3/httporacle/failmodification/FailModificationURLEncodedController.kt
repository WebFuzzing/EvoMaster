package com.foo.rest.examples.spring.openapi.v3.httporacle.failmodification

import com.foo.rest.examples.spring.openapi.v3.SpringController
import com.foo.rest.examples.spring.openapi.v3.httporacle.failmodification.urlencoded.UrlencodedFailModificationApplication
import com.foo.rest.examples.spring.openapi.v3.httporacle.failmodification.xml.XmlFailModificationApplication


class FailModificationURLEncodedController: SpringController(UrlencodedFailModificationApplication::class.java){
    override fun resetStateOfSUT() {
        UrlencodedFailModificationApplication.reset()
    }
}
