package com.foo.rest.examples.spring.openapi.v3.security.pathconflict

import com.foo.rest.examples.spring.openapi.v3.SpringController
import org.evomaster.client.java.controller.AuthUtils
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto

class PathConflictDeleteController : SpringController(PathConflictDeleteApplication::class.java) {

    override fun getInfoForAuthentication(): List<AuthenticationDto> {
        return listOf(
            AuthUtils.getForAuthorizationHeader("FOO","FOO"),
            AuthUtils.getForAuthorizationHeader("BAR","BAR"),
        )
    }

    override fun resetStateOfSUT() {
        PathConflictDeleteApplication.reset()
    }

}