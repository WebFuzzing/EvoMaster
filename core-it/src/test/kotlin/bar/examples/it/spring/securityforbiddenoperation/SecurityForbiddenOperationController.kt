package bar.examples.it.spring.securityforbiddenoperation

import bar.examples.it.spring.SpringController
import org.evomaster.client.java.controller.AuthUtils
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto
import org.evomaster.client.java.controller.problem.ProblemInfo
import org.evomaster.client.java.controller.problem.RestProblem

class SecurityForbiddenOperationController : SpringController(SecurityForbiddenOperationApplication::class.java){


    override fun getInfoForAuthentication(): List<AuthenticationDto> {
        return listOf(
            AuthUtils.getForAuthorizationHeader("FOO","FOO"),
            AuthUtils.getForAuthorizationHeader("BAR","BAR"),
        )
    }

    override fun resetStateOfSUT() {
        SecurityForbiddenOperationApplication.cleanState()
    }
}