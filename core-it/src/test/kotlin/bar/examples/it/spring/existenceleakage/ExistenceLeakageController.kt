package bar.examples.it.spring.existenceleakage

import bar.examples.it.spring.SpringController
import org.evomaster.client.java.controller.AuthUtils
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto

class ExistenceLeakageController : SpringController(ExistenceLeakageApplication::class.java) {

    override fun getInfoForAuthentication(): List<AuthenticationDto> {
        return listOf(
            AuthUtils.getForAuthorizationHeader("FOO","FOO"),
            AuthUtils.getForAuthorizationHeader("BAR","BAR"),
        )
    }
}