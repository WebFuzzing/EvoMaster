package org.evomaster.resource.rest.generator.implementation.java.em

import org.evomaster.resource.rest.generator.implementation.java.JavaClass
import org.evomaster.resource.rest.generator.model.AppClazz
import org.evomaster.resource.rest.generator.model.CommonTypes
import org.evomaster.resource.rest.generator.model.PropertySpecification
import org.evomaster.resource.rest.generator.template.DeclarationScript
import org.evomaster.resource.rest.generator.template.GeneralSymbol
import org.evomaster.resource.rest.generator.template.MethodScript
import org.evomaster.resource.rest.generator.template.RegisterType

/**
 * created by manzh on 2019-10-11
 */
class JavaEMController(specification: AppClazz, val appClazz : String, val sutPackagePrefix : String) : JavaClass<AppClazz>(specification) {

    companion object{
        val ctx = PropertySpecification(
                name = "ctx",
                type =  "ConfigurableApplicationContext",
                isId = false,
                autoGen = false,
                impactful = false
        )

//        val application = PropertySpecification(
//                name = "applicationClass",
//                type =  "Class<?>",
//                isId = false,
//                autoGen = false,
//                impactful = false
//        )


        val connection = PropertySpecification(
                name = "connection",
                type =  "Connection",
                isId = false,
                autoGen = false,
                impactful = false
        )

        val port = PropertySpecification(
                name = "controllerPort",
                type =  "int",
                isId = false,
                defaultValue = "40100")

        val declarations = listOf(
                CTXDeclaration(ctx),
                //ApplicationDeclaration(application),
                StaticDeclaration(port),
                SimpleDeclaration(connection),
                StaticDeclaration(PropertySpecification(
                        name = "embeddedStarter",
                        type =  "InstrumentedSutStarter",
                        isId = false
                )),
                StaticDeclaration(PropertySpecification(
                        name = "baseUrlOfSut",
                        type =  CommonTypes.STRING.toString(),
                        isId = false
                )),
                StaticDeclaration(PropertySpecification(
                        name = "controller",
                        type =  "SutController",
                        isId = false
                )),
                StaticDeclaration(PropertySpecification(
                        name = "remoteController",
                        type =  "RemoteController",
                        isId = false
                ))
        )
    }

    override fun generateConstructors(types: RegisterType): List<String> {



        return listOf(
                """
                ${formatBoundary(getBoundary())} ${getName()} ${GeneralSymbol.LEFT_PARENTHESIS}${GeneralSymbol.RIGHT_PARENTHESIS} ${GeneralSymbol.LEFT_BRACE}
                    this(${port.defaultValue});
                ${GeneralSymbol.RIGHT_BRACE}
                """.trimIndent()
                ,
                """
                    ${formatBoundary(getBoundary())} ${getName()} ${GeneralSymbol.LEFT_PARENTHESIS} ${declarations[1].generateAsVarOfConstructor(types)} ${GeneralSymbol.RIGHT_PARENTHESIS}${GeneralSymbol.LEFT_BRACE}
                    setControllerPort(${declarations[1].generateDefaultVarName()});
                    ${GeneralSymbol.RIGHT_BRACE}
                """.trimIndent())
    }

    override fun getImports(): List<String> = listOf(
            "org.evomaster.client.java.controller.internal.SutController",
            "org.evomaster.core.remote.service.RemoteController",
            "com.p6spy.engine.spy.P6SpyDriver",
            "org.springframework.boot.SpringApplication",
            "org.springframework.jdbc.core.JdbcTemplate",
            "org.evomaster.client.java.controller.EmbeddedSutController",
            "org.springframework.context.ConfigurableApplicationContext",
            "org.hibernate.dialect.H2Dialect",

            "org.evomaster.client.java.controller.ExternalSutController",
            "org.evomaster.client.java.controller.InstrumentedSutStarter",
            "org.evomaster.client.java.controller.db.DbCleaner",
            "org.evomaster.client.java.controller.problem.ProblemInfo",
            "org.evomaster.client.java.controller.problem.RestProblem",
            "org.evomaster.client.java.controller.api.dto.AuthenticationDto",
            "org.evomaster.client.java.controller.api.dto.SutInfoDto",
            "org.h2.tools.Server",
            "java.sql.Connection",
            "java.sql.DriverManager",
            "java.sql.SQLException",
            "java.util.*",

            "$sutPackagePrefix*"
    )

    override fun getMethods(): List<MethodScript> {
        return listOf(
                EMControllerMain(specification.name),
                GetProblemInfo(),
                GetInfoForAuthentication(),
                GetPreferredOutputFormat(),
                GetSutPort(),
                GetPackagePrefixesToCover(sutPackagePrefix),
                IsSutRunning(),
                GetDatabaseDriverName(),
                GetConnection(),
                StartSut(appClazz),
                StopSut(),
                ResetStateOfSut()
        )
    }

    override fun getDeclaration(): List<DeclarationScript> {
        if (specification.properties.isEmpty()){
            return declarations
        }
        TODO("NOT IMPLEMENT")
    }

    override fun getImplementedInterface(): List<String> = listOf()

    override fun getSuperClazz(): List<String>  = listOf("EmbeddedSutController")

    override fun getTags(): List<String>  = listOf()

    override fun getName(): String = specification.name

}