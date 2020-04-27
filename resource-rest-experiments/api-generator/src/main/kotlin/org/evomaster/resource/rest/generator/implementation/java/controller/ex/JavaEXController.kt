package org.evomaster.resource.rest.generator.implementation.java.controller.ex

import org.evomaster.resource.rest.generator.implementation.java.JavaClass
import org.evomaster.resource.rest.generator.implementation.java.controller.*
import org.evomaster.resource.rest.generator.model.AppClazz
import org.evomaster.resource.rest.generator.model.PropertySpecification
import org.evomaster.resource.rest.generator.template.DeclarationScript
import org.evomaster.resource.rest.generator.template.GeneralSymbol
import org.evomaster.resource.rest.generator.template.MethodScript
import org.evomaster.resource.rest.generator.template.RegisterType

/**
 * created by manzh on 2020-01-02
 */
class JavaEXController(specification: AppClazz, val rootProject: String, val csName : String, val jarName : String, val appClazz : String, val sutPackagePrefix : String) : JavaClass<AppClazz>(specification) {

    companion object{

        val connection = PropertySpecification(
                name = "connection",
                type =  "Connection",
                isId = false,
                autoGen = false,
                impactful = false
        )

        val h2 = PropertySpecification(
                name = "h2",
                type =  "Server",
                isId = false,
                autoGen = false,
                impactful = false
        )
        val timeInSeconds = PropertySpecification(
                name ="timeoutSeconds",
                type = "int",
                isId = false
        )

        val sutPort = PropertySpecification(
                name ="sutPort",
                type = "int",
                isId = false
        )
        val dbProt = PropertySpecification(
                name ="dbPort",
                type = "int",
                isId = false
        )
        val jarLoc = PropertySpecification(
                name ="jarLocation",
                type = "String",
                isId = false
        )

        val controllerPort = SimplePrivateDeclaration(PropertySpecification(
                name ="controllerPort",
                type = "int",
                isId = false
        ))

        val declarations = listOf(
                SimplePrivateDeclaration(
                        jarLoc, isFinalProperty = true
                ),
                SimplePrivateDeclaration(
                        sutPort, isFinalProperty = true
                ),
                SimplePrivateDeclaration(
                    timeInSeconds, isFinalProperty = true
                ),

                SimplePrivateDeclaration(
                        dbProt, isFinalProperty = true
                ),
                SimplePrivateDeclaration(connection),
                SimplePrivateDeclaration(h2)
        )
    }

    override fun generateConstructors(types: RegisterType): List<String> {

        return listOf(
                """
                    ${formatBoundary(getBoundary())} ${getName()} ()${GeneralSymbol.LEFT_BRACE}
                    this(40100,"$rootProject$csName/target/$jarName.jar",12345, 120);
                    ${GeneralSymbol.RIGHT_BRACE}
                """.trimIndent()
                ,
                """
                    ${formatBoundary(getBoundary())} ${getName()} (${controllerPort.generateAsVarOfConstructor(types)}, ${(0..2).joinToString(",") { declarations[it].generateAsVarOfConstructor(types) }})${GeneralSymbol.LEFT_BRACE}
                    ${(0..2).joinToString(";${System.lineSeparator()}") { "this.${declarations[it].getName()}=${declarations[it].generateDefaultVarName()}" }};
                    this.${declarations[3].getName()} = ${declarations[1].generateDefaultVarName()} + 1;
                    setControllerPort(${controllerPort.generateDefaultVarName()});
                    ${GeneralSymbol.RIGHT_BRACE}
                """.trimIndent())
    }

    override fun getImports(): List<String> = listOf(
            "com.p6spy.engine.spy.P6SpyDriver",
            "org.evomaster.client.java.controller.ExternalSutController",
            "org.evomaster.client.java.controller.InstrumentedSutStarter",
            "org.evomaster.client.java.controller.api.dto.AuthenticationDto",
            "org.evomaster.client.java.controller.api.dto.SutInfoDto",
            "org.evomaster.client.java.controller.db.DbCleaner",
            "org.evomaster.client.java.controller.problem.ProblemInfo",
            "org.evomaster.client.java.controller.problem.RestProblem",
            "org.h2.tools.Server",
            "org.hibernate.dialect.H2Dialect",

            "java.sql.Connection",
            "java.sql.DriverManager",
            "java.sql.SQLException",
            "java.util.List",

            "$sutPackagePrefix*"
    )

    override fun getMethods(): List<MethodScript> {
        return listOf(
                EXControllerMain(exClazz = specification.name, jarName = jarName, csName = csName, rootProject = rootProject),
                DbUrlMethod(csName),
                GetInputParametersMethod(),
                GetJVMParametersMethod(),
                GetBaseURLMethod(sutPort.name),
                GetPathToExecutableJarMethod(jarLoc.name),
                GetLogMsgOfInitServerMethod(appClazz),
                GetMaxAwaitForInitInSecondsMethod(timeInSeconds.name),
                PreStartMethod(h2.name),
                PostStartMethod(),
                ResetStateOfSut(connection.name),
                PreStopMethod(),
                PostStopMethod(h2.name),
                CloseDbConnectionMethod(connection.name),
                GetPackagePrefixesToCover(sutPackagePrefix),
                GetProblemInfo(isEx = true),
                GetPreferredOutputFormat(),
                GetInfoForAuthentication(),
                GetConnection(connection.name),
                GetH2DatabaseDriverName()
        )
    }

    override fun getDeclaration(): List<DeclarationScript> {
        if (specification.properties.isEmpty()){
            return declarations
        }
        TODO("NOT IMPLEMENT")
    }

    override fun getImplementedInterface(): List<String> = listOf()

    override fun getSuperClazz(): List<String>  = listOf("ExternalSutController")

    override fun getTags(): List<String>  = listOf()

    override fun getName(): String = specification.name
}