package org.evomaster.resource.rest.generator.pom

import org.apache.maven.model.Build
import org.apache.maven.model.Dependency
import org.apache.maven.model.Model
import org.apache.maven.model.io.xpp3.MavenXpp3Writer
import java.io.FileWriter
import java.nio.file.Files
import java.nio.file.Paths

/**
 * created by manzh on 2019-08-21
 */
abstract class POModel (val groupId : String, val artifactId: String, val version: String, val name : String = "pom.xml", val output: String = ""){
    abstract fun getPOModel() : Model

    fun save() {
        val folder = "$output${if (output.isNotBlank() && !output.endsWith(System.getProperty("file.separator"))) System.getProperty("file.separator") else ""}"
        if (folder.isNotBlank())Files.createDirectories(Paths.get(folder))
        val writer = FileWriter("$folder$name")

        MavenXpp3Writer().write(writer, getPOModel())
    }
}

class CSPOModel(groupId: String, artifactId: String, version: String = "0.0.1", output : String, val parent : PackagedPOModel?=null) : POModel(groupId= groupId, artifactId= artifactId, version = version, output=output){

    override fun getPOModel(): Model {
        val model = Model()
        model.groupId = groupId
        model.artifactId = artifactId
        //model.packaging = "jar"
        model.modelVersion = "4.0.0"
        model.version = version
        model.dependencies.add(DependencyManager.JAVAX_VALIDATION_API.getDependency())
        model.dependencies.add(DependencyManager.JAVAX_WS_RS_API.getDependency())
        model.dependencies.add(DependencyManager.SPRING_BOOT_STARTER_WEB.getDependency())
        model.dependencies.add(DependencyManager.SPRING_BOOT_STARTER_DATA_JPA.getDependency())
        model.dependencies.add(DependencyManager.SPRING_BOOT_STARTER_SECURITY.getDependency())
        model.dependencies.add(DependencyManager.H2DB.getDependency())
        model.dependencies.add(DependencyManager.P6SPY.getDependency())
        model.dependencies.add(DependencyManager.IO_SPRINGFOX_SWAGGER2.getDependency(exclusions = mutableListOf(Pair("io.swagger", "*"))))
        model.dependencies.add(DependencyManager.IO_SPRINGFOX_SPRING_WEB.getDependency())
        model.dependencies.add(DependencyManager.IO_SWAGGER_PARSER.getDependency())
        model.dependencies.add(DependencyManager.SPRING_BOOT.getDependency())
        model.build = Build()
        model.build.plugins.add(DependencyManager.MAVEN_PLUGINS_COMPILER.getPlugin(configuration = mutableMapOf("source" to "1.8", "target" to "1.8")))

        return model
    }
}

class EMPOModel(groupId: String, artifactId: String, version: String = "0.0.1", output : String, val csPOModel : CSPOModel, val parent : PackagedPOModel?=null)
    : POModel(groupId= groupId, artifactId= artifactId, version = version, output=output){
    override fun getPOModel(): Model {
        val model = Model()
        model.groupId = groupId
        model.artifactId = artifactId
        model.version = version
        model.packaging = "jar"
        model.dependencies.add(DependencyManager.EVOMASTER_CLIENT_JAVA_CONTROLLER.getDependency())
        model.dependencies.add(DependencyManager.EVOMASTER_CLIENT_JAVA_INSTRUMENTATION.getDependency())
        model.dependencies.add(DependencyManager.IO_REST_ASSURED.getDependency())
        model.dependencies.add(DependencyManager.HAMCREST.getDependency())
        model.dependencies.add(DependencyManager.JUNIT.getDependency())
        model.dependencies.add(ArtifactTemplate(artifactId = csPOModel.artifactId, groupId = csPOModel.groupId).getDependency(version=csPOModel.version))
        model.build = Build()
        model.build.plugins.add(DependencyManager.MAVEN_PLUGINS_COMPILER.getPlugin(configuration = mutableMapOf("score" to "1.8", "target" to "1.8")))

        return model
    }
}

class PackagedPOModel(val modules : MutableList<String>, groupId: String, artifactId: String, version: String = "0.0.1", output : String)
    : POModel(groupId= groupId, artifactId= artifactId, version = version, output=output){
    override fun getPOModel(): Model {
        val model = Model()
        model.groupId = groupId
        model.artifactId = artifactId
        model.version = version
        model.packaging = "pom"

        model.modules.addAll(modules)

        return model
    }

}

fun main(args : Array<String>){
    CSPOModel("test", "test", output = "hh/hh").save()
}

