package com.foo.rest.examples.spring.openapi.v3.httporacle.failmodification

import com.foo.rest.examples.spring.openapi.v3.SpringController
import com.foo.rest.examples.spring.openapi.v3.httporacle.failmodification.unauthorized.FailModificationUnauthApplication
import org.evomaster.client.java.controller.AuthUtils
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto


class FailModificationUnauthorizedController: SpringController(FailModificationUnauthApplication::class.java){

    override fun getInfoForAuthentication(): List<AuthenticationDto> {
        return listOf(
            AuthUtils.getForAuthorizationHeader("FOO","FOO"),
            AuthUtils.getForAuthorizationHeader("BAR","BAR"),
        )
    }

    override fun resetStateOfSUT() {
        FailModificationUnauthApplication.reset()
    }
}
