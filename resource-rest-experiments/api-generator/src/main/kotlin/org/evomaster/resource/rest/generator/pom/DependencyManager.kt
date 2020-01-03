package org.evomaster.resource.rest.generator.pom

import org.apache.maven.model.Dependency
import org.apache.maven.model.Exclusion
import org.apache.maven.model.Plugin
import org.apache.maven.model.PluginExecution
import org.codehaus.plexus.util.xml.Xpp3Dom

/**
 * created by manzh on 2019-08-21
 */

object DependencyManager{

    val defined_version = mutableMapOf(
            "project.build.sourceEncoding" to "UTF-8",
            "evomaster.version" to "0.3.1-SNAPSHOT",
            "java.version" to "1.8",
            "kotlin.version" to "1.3.21",
            "kotlin.compiler.incremental" to "true",
            "junit.jupiter.version" to "5.5.0",
            "junit.platform.version" to "1.5.0",
            "springboot.version" to "2.0.3.RELEASE",
            //"springframework.version" to "2.0.3.RELEASE",
            "springfox.version" to "2.9.2",
            "swagger.version" to "1.5.21",
            "swagger.parser.version" to "1.0.39",
            "javax.validation.version" to "2.0.1.Final",
            "javax.el.version" to "2.2.5",
            "javax.ws.rs.version" to "2.1.1",
            "h2database.version" to "1.4.197",
            "p6spy.version" to "3.6.0",
            "hamcrest.version" to "1.3",
            "restassured.version" to "3.0.2",
            "junit.version" to "4.12",
            "maven.compiler.version" to "3.5.1",
            "maven.shade.version" to "3.0.0"
    )

    val JAVAX_VALIDATION_API = ArtifactTemplate("javax.validation", "validation-api", "javax.validation.version")
    val JAVAX_WS_RS_API = ArtifactTemplate("javax.ws.rs", "javax.ws.rs-api", "javax.ws.rs.version")

    val SPRING_BOOT = ArtifactTemplate("org.springframework.boot", "spring-boot", "springboot.version")
    val SPRING_BOOT_STARTER_WEB = ArtifactTemplate("org.springframework.boot", "spring-boot-starter-web", "springboot.version")
    val SPRING_BOOT_STARTER_DATA_JPA = ArtifactTemplate("org.springframework.boot", "spring-boot-starter-data-jpa", "springboot.version")
    val SPRING_BOOT_STARTER_SECURITY = ArtifactTemplate("org.springframework.boot", "spring-boot-starter-security", "springboot.version")

    val SPRING_BOOT_MAVEN_PLUGIN = ArtifactTemplate("org.springframework.boot", "spring-boot-maven-plugin", "springboot.version")

    val IO_SPRINGFOX_SWAGGER2 = ArtifactTemplate("io.springfox", "springfox-swagger2", "springfox.version")
    val IO_SPRINGFOX_SPRING_WEB = ArtifactTemplate("io.springfox", "springfox-spring-web", "springfox.version")

    val IO_SWAGGER_PARSER = ArtifactTemplate("io.swagger", "swagger-parser", "swagger.parser.version")

    val KOTLIN_STDLIB = ArtifactTemplate("org.jetbrains.kotlin", "kotlin-stdlib", "kotlin.version")
    val KOTLIN_MAVEN_PLUGIN = ArtifactTemplate("org.jetbrains.kotlin", "kotlin-maven-plugin", "kotlin.version")

    val H2DB = ArtifactTemplate("com.h2database", "h2", "h2database.version")

    val P6SPY = ArtifactTemplate("p6spy", "p6spy", "p6spy.version")

    val EVOMASTER_CORE = ArtifactTemplate("org.evomaster", "evomaster-core", "evomaster.version")
    val EVOMASTER_CLIENT_JAVA_CONTROLLER = ArtifactTemplate("org.evomaster", "evomaster-client-java-controller", "evomaster.version")
    val EVOMASTER_CLIENT_JAVA_INSTRUMENTATION = ArtifactTemplate("org.evomaster", "evomaster-client-java-instrumentation", "evomaster.version")
    val EVOMASTER_CLIENT_DADABASE_SPY = ArtifactTemplate("org.evomaster", "evomaster-client-database-spy", "evomaster.version")

    val IO_REST_ASSURED = ArtifactTemplate("io.rest-assured", "rest-assured", "restassured.version")

    val HAMCREST = ArtifactTemplate("org.hamcrest", "hamcrest-all", "hamcrest.version")

    val JUNIT = ArtifactTemplate("junit", "junit", "junit.version")

    val MAVEN_PLUGINS_COMPILER = ArtifactTemplate("org.apache.maven.plugins", "maven-compiler-plugin", "maven.compiler.version")

    val MAVEN_PLUGINS_SHADE = ArtifactTemplate("org.apache.maven.plugins", "maven-shade-plugin", "maven.shade.version")

    val JUNIT_JUPITER_PARAMS = ArtifactTemplate("org.junit.jupiter", "junit-jupiter-params", "junit.jupiter.version")

    val JUNIT_JUPITER_ENGINE = ArtifactTemplate("org.junit.jupiter", "junit-jupiter-engine", "junit.jupiter.version")

    val JUNIT_JUPITER_PLATFORM = ArtifactTemplate("org.junit.platform", "junit-platform-launcher", "junit.platform.version")

}

class ArtifactTemplate(val groupId : String, val artifactId : String, val versionKey : String = "", val scope: String = Scope.ALL.name) {

    fun getPluginExecution(phase : String = "", id :String = "", goals : MutableList<String> = mutableListOf(), configuration : MutableMap<String, String> = mutableMapOf()) : PluginExecution{
        return PluginExecution().also {
            if (phase.isNotBlank()) it.phase = phase
            if (goals.isNotEmpty()) it.goals.addAll(goals)
            if (configuration.isNotEmpty()) it.configuration = configuration
        }
    }

    fun getPlugin(version : String = "", configuration: MutableMap<String, String> = mutableMapOf(), executions : MutableList<PluginExecution> = mutableListOf()) : Plugin{
        return Plugin().also {
            it.artifactId = artifactId
            it.groupId = groupId
            if (version.isNotBlank()) it.version = version
            if (configuration.isNotEmpty()) {
                val config = Xpp3Dom("configuration")
                configuration.forEach { (t, u) ->
                    config.addChild(Xpp3Dom(t).also {c-> c.value = u })
                }
                it.configuration = config
            }
            it.executions.addAll(executions)
        }
    }

    fun getDependency(version : String = "", scope : String = "", exclusions: MutableList<Pair<String, String>> = mutableListOf()) : Dependency{
        val dep = Dependency()
        dep.artifactId = artifactId
        dep.groupId = groupId
        dep.version = if (version.isNotBlank()) version else DependencyManager.defined_version[versionKey]?: throw IllegalArgumentException("undefined version")

        if (scope.isNotBlank())
            dep.scope = scope

        exclusions.forEach {
            dep.exclusions.add(Exclusion().also { e->
                e.groupId = it.first
                e.artifactId = it.second
            })
        }
        return dep
    }
}

enum class Scope(name : String){
    ALL(""),
    TEST("test"),
    PROVIDED ("provided")
}
