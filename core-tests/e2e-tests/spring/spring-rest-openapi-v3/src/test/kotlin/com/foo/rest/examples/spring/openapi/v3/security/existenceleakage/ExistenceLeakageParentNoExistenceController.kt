package com.foo.rest.examples.spring.openapi.v3.security.existenceleakage

import com.foo.rest.examples.spring.openapi.v3.SpringController
import com.foo.rest.examples.spring.openapi.v3.security.existenceleakage.parentauth.ExistenceLeakageParentApplication
import com.foo.rest.examples.spring.openapi.v3.security.existenceleakage.parentnoexistence.ExistenceLeakageParentNoExistenceApplication
import org.evomaster.client.java.controller.AuthUtils
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto

class ExistenceLeakageParentNoExistenceController : SpringController(ExistenceLeakageParentNoExistenceApplication::class.java) {

    override fun getInfoForAuthentication(): List<AuthenticationDto> {
        return listOf(
            AuthUtils.getForAuthorizationHeader("FOO","FOO"),
            AuthUtils.getForAuthorizationHeader("BAR","BAR"),
        )
    }

    override fun resetStateOfSUT() {
        ExistenceLeakageParentNoExistenceApplication.reset()
    }

}