package bar.examples.it.spring

import org.evomaster.client.java.controller.EmbeddedSutController
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto
import org.evomaster.client.java.controller.api.dto.SutInfoDto
import org.evomaster.client.java.sql.DbSpecification
import org.evomaster.client.java.controller.problem.ProblemInfo
import org.evomaster.client.java.controller.problem.RestProblem
import org.springframework.boot.SpringApplication
import org.springframework.context.ConfigurableApplicationContext


abstract class SpringController(protected val applicationClass: Class<*>) : EmbeddedSutController() {

    init {
        super.setControllerPort(0)
    }


    protected var ctx: ConfigurableApplicationContext? = null

    override fun startSut(): String {
        ctx = SpringApplication.run(applicationClass, "--server.port=0")
        return "http://localhost:$sutPort"
    }

    protected val sutPort: Int
        get() = (ctx!!.environment
                .propertySources["server.ports"].source as Map<*, *>)["local.server.port"] as Int

    override fun isSutRunning(): Boolean {
        return ctx != null && ctx!!.isRunning
    }

    override fun stopSut() {
        ctx?.stop()
        ctx?.close()
    }

    override fun getPackagePrefixesToCover(): String {
        return "bar.foo."
    }

    override fun resetStateOfSUT() { //nothing to do
    }

    override fun getProblemInfo(): ProblemInfo {
        return RestProblem(
                "http://localhost:$sutPort/v3/api-docs",
                null
        )
    }

    override fun getInfoForAuthentication(): List<AuthenticationDto> {
        return listOf()
    }

    override fun getDbSpecifications(): MutableList<DbSpecification>? {
        return null
    }


    override fun getPreferredOutputFormat(): SutInfoDto.OutputFormat {
        return SutInfoDto.OutputFormat.KOTLIN_JUNIT_5
    }

}