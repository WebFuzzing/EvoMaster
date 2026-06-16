package com.foo.rest.examples.bb.httppatch

import com.foo.rest.examples.bb.SpringController

class HttpPatchController : SpringController(HttpPatchApplication::class.java) {

    /**
     * Re-seed the in-memory store before each EvoMaster action sequence, so that destructive
     * JSON Patch documents (e.g. removing a required field) cannot corrupt the state used by
     * subsequent calls. This is what keeps the e2e test deterministic and the SUT robust.
     */
    override fun resetStateOfSUT() {
        HttpPatchApplication.reset()
    }
}
