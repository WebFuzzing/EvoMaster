package org.evomaster.resource.rest.generator.template

import org.evomaster.resource.rest.generator.implementation.java.service.IfSnippet
import java.nio.file.Files
import java.nio.file.Paths

/**
 * created by manzh on 2019-08-13
 */
interface ClassTemplate : ScriptTemplate {

    fun generateHeading(types : RegisterType) : String

    fun generateEnding(types : RegisterType) : String

    fun getOutputSrcFolder() : String

    fun getOutputResourceFolder() : String

    fun getFileSuffix() : String

    fun getFileName() : String

    fun getPackage() : String

    fun getImports() : List<String>

    fun getMethods() : List<out MethodScript>

    fun getDeclaration() : List<out DeclarationScript>

    fun includeDeclaration() : Boolean

    fun generateConstructors(types : RegisterType) : List<String>

    fun getImplementedInterface() : List<String>

    fun getSuperClazz() : List<String>

    fun generateSuperClazz(types: RegisterType) : List<String> = getSuperClazz()

    fun isAbstract() : Boolean

    fun getType() : ClassType = ClassType.CLAZZ

    fun generateImports() : List<String>

    fun generatePackage() : String

    fun getComments() : List<String>

    override fun generate(types : RegisterType): String {
        var content = StringBuilder()

        //package
        if (getPackage().isNotBlank()) {
            content.append(generatePackage())
            content.append(System.lineSeparator())
        }

        //import
        generateImports().forEach {
            if (!it.isNullOrBlank()){
                content.append(it)
                content.append(System.lineSeparator())
            }
        }

        getComments().forEach {
            if (!it.isNullOrBlank()){
                content.append(it)
                content.append(System.lineSeparator())
            }
        }

        getTags().forEach {
            if (!it.isNullOrBlank()){
                content.append(it)
                content.append(System.lineSeparator())
            }
        }
        content.append(generateHeading(types))

        content.append(System.lineSeparator())

        generateConstructors(types).forEach {
            if (!it.isNullOrBlank()){
                content.append(it)
                content.append(System.lineSeparator())
            }
        }

        if (includeDeclaration()){
            getDeclaration().forEach { d ->
                val text = d.generate(types)
                if (!text.isNullOrBlank()){
                    content.append(d.generate(types))
                    content.append(System.lineSeparator())
                }
            }
        }

        getMethods().forEach { d ->
            val text = d.generate(types)
            if (!text.isNullOrBlank()){
                content.append(d.generate(types))
                content.append(System.lineSeparator())
            }
        }

        content.append(System.lineSeparator())
        content.append(generateEnding(types))

        return formatScript(content.toString())
    }

    fun getTags() : List<String>

    fun formatScript(content : String) : String = content

    fun generateAndSave(types : RegisterType){
        val targetFolder = getOutputSrcFolder().run { if (this.endsWith(System.getProperty("file.separator"))) this else "$this${System.getProperty("file.separator")}" } + getPackage().replace(".", System.getProperty("file.separator")) + System.getProperty("file.separator")
        Files.createDirectories(Paths.get(targetFolder))
        val content = generate(types)
        detectAndSaveBranchIds(content)
        Files.write(Paths.get( targetFolder + getFileName() + "." +getFileSuffix()), content.toByteArray())
    }

    fun detectAndSaveBranchIds(content: String){
        val allIf = getMethods().flatMap { m -> m.getIfSnippets().also { it.forEach { e-> e.methodName = m.getName() } } }
        if (allIf.isEmpty()) return

        val lines = content.split(System.lineSeparator())
        var indexToDetect = 0
        var index = 0
        for (s in lines){
            var fs = allIf[indexToDetect].snippet.replace(" ","")
            var ft = s.replace(" ","")
            if(!ft.isBlank() && (fs.contains(ft) || ft.contains(fs))){
                allIf[indexToDetect].line = index+1
                allIf[indexToDetect].clazz = "${getPackage()}.${getName()}"
                indexToDetect++
                if(indexToDetect == allIf.size) break
            }
            index++
        }
        val targets = listOf(IfSnippet.getHeader()).plus(allIf.map { it.toCSV() }).joinToString(System.lineSeparator())

        val targetFile = if(indexToDetect < allIf.size){
            "targets_incomplete"
        }else{
            "targets"
        }
        val targetFolder =  getOutputResourceFolder().run { if (this.endsWith(System.getProperty("file.separator"))) this else "$this${System.getProperty("file.separator")}" }
        Files.createDirectories(Paths.get(targetFolder))
        Files.write(Paths.get("$targetFolder$targetFile.csv"), targets.toByteArray())
    }
}