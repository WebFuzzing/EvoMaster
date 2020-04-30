package org.evomaster.resource.rest.generator.pom

import org.apache.maven.model.Build
import org.apache.maven.model.Model
import org.apache.maven.model.PluginExecution
import org.apache.maven.model.io.xpp3.MavenXpp3Writer
import org.codehaus.plexus.util.xml.Xpp3Dom
import java.io.FileWriter
import java.nio.file.Files
import java.nio.file.Paths

/**
 * created by manzh on 2019-08-21
 */
abstract class POModel (val groupId : String, val artifactId: String, val version: String, val name : String = "pom.xml", val output: String = ""){
    companion object{
        const val DEFAULT_VERSION = "0.0.1-SNAPSHOT"
    }
    abstract fun getPOModel() : Model

    fun save() {
        val folder = "$output${if (output.isNotBlank() && !output.endsWith(System.getProperty("file.separator"))) System.getProperty("file.separator") else ""}"
        if (folder.isNotBlank())Files.createDirectories(Paths.get(folder))
        val writer = FileWriter("$folder$name")

        MavenXpp3Writer().write(writer, getPOModel())
    }
}

class CSPOModel(groupId: String, artifactId: String, version: String = DEFAULT_VERSION, output : String, val repackageName : String? = null, val parent : PackagedPOModel?=null) : POModel(groupId= groupId, artifactId= artifactId, version = version, output=output){

    override fun getPOModel(): Model {
        val model = Model()
        model.groupId = groupId
        model.artifactId = artifactId
        //model.packaging = "jar"
        model.modelVersion = "4.0.0"
        model.version = version
        model.dependencies.add(DependencyManager.EVOMASTER_CLIENT_DADABASE_SPY.getDependency())
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
        val execution = PluginExecution()
        execution.addGoal("repackage")
        val fn = Xpp3Dom("finalName")
        fn.value = repackageName?:artifactId
        val classifier =  Xpp3Dom("classifier")
        classifier.value = "sut"
        val econfig = Xpp3Dom("configuration")
        econfig.addChild(fn)
        econfig.addChild(classifier)
        execution.configuration = econfig

        model.build.plugins.add(DependencyManager.SPRING_BOOT_MAVEN_PLUGIN.getPlugin(
                executions = mutableListOf(execution)
        ))
        return model
    }
}

class EMPOModel(groupId: String, artifactId: String, version: String = DEFAULT_VERSION, output : String, val csPOModel : CSPOModel, val parent : PackagedPOModel?=null)
    : POModel(groupId= groupId, artifactId= artifactId, version = version, output=output){
    override fun getPOModel(): Model {
        val model = Model()
        model.groupId = groupId
        model.artifactId = artifactId
        model.version = version
        model.packaging = "jar"
        model.modelVersion = "4.0.0"
//        model.dependencies.add(DependencyManager.EVOMASTER_CORE.getDependency())
        model.dependencies.add(DependencyManager.EVOMASTER_CLIENT_JAVA_CONTROLLER.getDependency())
        model.dependencies.add(DependencyManager.EVOMASTER_CLIENT_JAVA_INSTRUMENTATION.getDependency())
        model.dependencies.add(DependencyManager.IO_REST_ASSURED.getDependency())
        model.dependencies.add(DependencyManager.HAMCREST.getDependency())
        model.dependencies.add(DependencyManager.JUNIT.getDependency())
        model.dependencies.add(DependencyManager.JUNIT_JUPITER_ENGINE.getDependency())
        model.dependencies.add(DependencyManager.JUNIT_JUPITER_PARAMS.getDependency())
        model.dependencies.add(DependencyManager.JUNIT_JUPITER_PLATFORM.getDependency())

        model.dependencies.add(ArtifactTemplate(artifactId = csPOModel.artifactId, groupId = csPOModel.groupId).getDependency(version=csPOModel.version))
        model.build = Build()
        model.build.plugins.add(DependencyManager.MAVEN_PLUGINS_COMPILER.getPlugin(configuration = mutableMapOf("source" to "1.8", "target" to "1.8")))

        return model
    }
}

class EXPOModel(groupId: String, artifactId: String, version: String = DEFAULT_VERSION, output : String, val repackageName : String? = null, val exClazz: String, val csPOModel : CSPOModel, val parent : PackagedPOModel?=null)
    : POModel(groupId= groupId, artifactId= artifactId, version = version, output=output){
    override fun getPOModel(): Model {
        val model = Model()
        model.groupId = groupId
        model.artifactId = artifactId
        model.version = version
        model.packaging = "jar"
        model.modelVersion = "4.0.0"
//        model.dependencies.add(DependencyManager.EVOMASTER_CORE.getDependency())
        model.dependencies.add(DependencyManager.EVOMASTER_CLIENT_JAVA_CONTROLLER.getDependency())
        model.dependencies.add(DependencyManager.EVOMASTER_CLIENT_JAVA_INSTRUMENTATION.getDependency())
        model.dependencies.add(DependencyManager.IO_REST_ASSURED.getDependency())
        model.dependencies.add(DependencyManager.HAMCREST.getDependency())
        model.dependencies.add(DependencyManager.JUNIT.getDependency())
        model.dependencies.add(DependencyManager.JUNIT_JUPITER_ENGINE.getDependency())
        model.dependencies.add(DependencyManager.JUNIT_JUPITER_PARAMS.getDependency())
        model.dependencies.add(DependencyManager.JUNIT_JUPITER_PLATFORM.getDependency())

        model.dependencies.add(ArtifactTemplate(artifactId = csPOModel.artifactId, groupId = csPOModel.groupId).getDependency(version=csPOModel.version))
        model.build = Build()
        model.build.plugins.add(DependencyManager.MAVEN_PLUGINS_COMPILER.getPlugin(configuration = mutableMapOf("source" to "1.8", "target" to "1.8")))

        val execution = PluginExecution()
        execution.phase = "package"
        execution.addGoal("shade")

        val fn = Xpp3Dom("finalName")
        fn.value = repackageName?:artifactId

        val transfers = Xpp3Dom("transformers")
        val transfer = Xpp3Dom("transformer")
        transfer.setAttribute("implementation","org.apache.maven.plugins.shade.resource.ManifestResourceTransformer")
        val entries = Xpp3Dom("manifestEntries")
        val main = Xpp3Dom("Main-Class")
        main.value = exClazz
        val premain = Xpp3Dom("Premain-Class")
        premain.value = "org.evomaster.client.java.instrumentation.InstrumentingAgent" // instrumentation agent class
        val agentemain = Xpp3Dom("Agent-Class")
        agentemain.value = "org.evomaster.client.java.instrumentation.InstrumentingAgent" // instrumentation agent class
        val redefined = Xpp3Dom("Can-Redefine-Classes")
        redefined.value = "true"
        val retransform = Xpp3Dom("Can-Retransform-Classes")
        retransform.value = "true"
        entries.addChild(main)
        entries.addChild(premain)
        entries.addChild(agentemain)
        entries.addChild(redefined)
        entries.addChild(retransform)
        transfer.addChild(entries)
        transfers.addChild(transfer)

        val econfig = Xpp3Dom("configuration")
        econfig.addChild(fn)
        econfig.addChild(transfers)
        execution.configuration = econfig
        model.build.plugins.add(DependencyManager.MAVEN_PLUGINS_SHADE.getPlugin(
                executions = mutableListOf(execution)
        ))

        return model
    }
}

class PackagedPOModel(val modules : List<String>, groupId: String, artifactId: String, version: String = DEFAULT_VERSION, output : String)
    : POModel(groupId= groupId, artifactId= artifactId, version = version, output=output){

    override fun getPOModel(): Model {
        val model = Model()
        model.modelVersion = "4.0.0"
        model.groupId = groupId
        model.artifactId = artifactId
        model.version = version
        model.packaging = "pom"

        model.modules.addAll(modules)

        return model
    }

}

fun main(args : Array<String>){
    val cs = CSPOModel("test", "testCs", output = "hh/hh/cs")
    cs.save()
    EXPOModel("test", "testEx", output = "hh/hh/ex", exClazz = "tests", csPOModel = cs).save()
}

