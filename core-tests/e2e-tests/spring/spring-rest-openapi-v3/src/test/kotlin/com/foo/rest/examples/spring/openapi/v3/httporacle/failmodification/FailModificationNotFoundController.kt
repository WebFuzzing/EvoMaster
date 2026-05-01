package com.foo.rest.examples.spring.openapi.v3.httporacle.failmodification

import com.foo.rest.examples.spring.openapi.v3.SpringController
import com.foo.rest.examples.spring.openapi.v3.httporacle.failmodification.notfound.FailModificationNotFoundApplication


class FailModificationNotFoundController: SpringController(FailModificationNotFoundApplication::class.java){
    override fun resetStateOfSUT() {
        FailModificationNotFoundApplication.reset()
    }
}
