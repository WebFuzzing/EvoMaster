package com.foo.rest.examples.spring.openapi.v3.httporacle.failmodification

import com.foo.rest.examples.spring.openapi.v3.SpringController
import com.foo.rest.examples.spring.openapi.v3.httporacle.failmodification.forbidden.FailModificationForbiddenApplication
import org.evomaster.client.java.controller.AuthUtils
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto


class FailModificationForbiddenController: SpringController(FailModificationForbiddenApplication::class.java){

    override fun getInfoForAuthentication(): List<AuthenticationDto> {
        return listOf(
            AuthUtils.getForAuthorizationHeader("FOO","FOO"),
            AuthUtils.getForAuthorizationHeader("BAR","BAR"),
        )
    }

    override fun resetStateOfSUT() {
        FailModificationForbiddenApplication.reset()
    }
}
