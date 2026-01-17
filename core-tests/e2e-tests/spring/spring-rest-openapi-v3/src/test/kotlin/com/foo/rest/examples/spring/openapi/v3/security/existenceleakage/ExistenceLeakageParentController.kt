package com.foo.rest.examples.spring.openapi.v3.security.existenceleakage

import com.foo.rest.examples.spring.openapi.v3.SpringController
import com.foo.rest.examples.spring.openapi.v3.security.existenceleakage.parentauth.ExistenceLeakageParentApplication
import org.evomaster.client.java.controller.AuthUtils
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto

class ExistenceLeakageParentController : SpringController(ExistenceLeakageParentApplication::class.java) {

    override fun getInfoForAuthentication(): List<AuthenticationDto> {
        return listOf(
            AuthUtils.getForAuthorizationHeader("FOO","FOO"),
            AuthUtils.getForAuthorizationHeader("BAR","BAR"),
        )
    }

    override fun resetStateOfSUT() {
        ExistenceLeakageParentApplication.reset()
    }

}