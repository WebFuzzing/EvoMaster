package org.evomaster.core.config

import com.fasterxml.jackson.dataformat.toml.TomlMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto
import java.io.File
import java.lang.reflect.Field
import java.lang.reflect.ParameterizedType
import kotlin.io.path.Path
import kotlin.io.path.exists

object ConfigUtil {

    /**
     * Either in TOML or YAML format
     */
    fun readFromFile(stringPath: String) : ConfigsFromFile{

        val path = Path(stringPath)
        if(!path.exists()){
            throw IllegalArgumentException("Config file does not exist: $path")
        }

        val mapper = if(isToml(stringPath)) {
            TomlMapper()
        } else if(isYaml(stringPath)){
            YAMLMapper()
        } else {
            throw IllegalArgumentException("Specified configuration file path is not of valid type." +
                    " Supported types are YAML and TOML. Wrong path: $stringPath")
        }

        val cff = try{
            mapper.readValue(path.toFile(), ConfigsFromFile::class.java)
        } catch (e: Exception){
            throw IllegalArgumentException("Failed to parse config file at: ${path.toAbsolutePath()}", e)
        }

        return cff;
    }

    private fun isYaml(stringPath: String) = stringPath.endsWith(".yml", true) || stringPath.endsWith(".yaml", true)

    private fun isToml(stringPath: String) = stringPath.endsWith(".toml", true)

    /**
     * The type of config file will depend on the [stringPath] extension, eg, .yaml or .toml
     */
    fun createConfigFileTemplate(stringPath: String, template: ConfigsFromFile) {

        val path = Path(stringPath)
        if(path.exists()){
            throw IllegalArgumentException("Config file already exists at: ${path.toAbsolutePath()}")
        }
        if(!isToml(stringPath) && !isYaml(stringPath)){
            throw IllegalArgumentException("Configuration file name does not end in a supported type, ie, .yaml or .toml: $stringPath")
        }

        val file = path.toFile().absoluteFile
        file.parentFile.mkdirs()
        file.createNewFile()

        file.appendText("### Template configuration file for EvoMaster.\n")
        file.appendText("### Most important parameters are already present here, commented out.\n")
        file.appendText("### Note that there are more parameters that can be configured. For a full list, see:\n")
        file.appendText("### https://github.com/EMResearch/EvoMaster/blob/master/docs/options.md\n")
        file.appendText("### or check them with the --help option.\n")
        file.appendText("\n")
        file.appendText("\n")


        if(isToml(stringPath)) {
            file.appendText("[configs]\n")
            template.configs
                    .toSortedMap()
                    .forEach {
                        file.appendText("# ${it.key}=${it.value}\n")
                    }
        }
        if(isYaml(stringPath)){
            file.appendText("configs:  {} # remove this {} when specifying properties\n")
            template.configs
                    .toSortedMap()
                    .forEach {
                        file.appendText("#   ${it.key}: ${it.value}\n")
                    }
        }


        file.appendText("\n\n\n")
        file.appendText("### Authentication configurations.\n")
        file.appendText("### For each possible registered user, can provide an ${AuthenticationDto::class.simpleName}" +
                " object to define how to log them in.\n")
        file.appendText("### Different types of authentication mechanisms can be configured here.\n")
        file.appendText("### For more information, read: https://github.com/EMResearch/EvoMaster/blob/master/docs/auth.md\n")
        file.appendText("\n")

        val auth = "auth"

        if(isToml(stringPath)) {
            AuthenticationDto::class.java.fields
                    .filter { it.name != "name" }
                    .forEach {
                        file.appendText("# [[$auth]]\n")
                        file.appendText("# name=?\n")
                        printObjectDefinition(false, file, auth, it)
                        file.appendText("\n")
                    }
        }
        if(isYaml(stringPath)){
            file.appendText("#auth:\n")
            val indent = "    "
            file.appendText("#  - name: ?\n")
            printObjectDefinition(true, file, indent, AuthenticationDto::class.java.getField("fixedHeaders"))
            printObjectDefinition(true, file, indent, AuthenticationDto::class.java.getField("loginEndpointAuth"))
        }

        file.appendText("\n")
    }

    private fun printObjectDefinition(isYaml: Boolean, file: File, prefix: String, field: Field){

        var isCollection = false

        val type = if(List::class.java.isAssignableFrom(field.type)
            || Set::class.java.isAssignableFrom(field.type)
            || field.type.isArray){
            isCollection = true
            (field.genericType as ParameterizedType).actualTypeArguments[0] as Class<*>
        } else {
            field.type
        }

        val sep = if(isYaml) ":" else "="

        if(java.lang.Boolean::class.java.isAssignableFrom(type) || java.lang.Boolean.TYPE == type){
            printIndentation(file, isYaml, prefix)
            file.appendText("${field.name}$sep true | false\n")
        } else if(java.lang.String::class.java.isAssignableFrom(type)
            || java.lang.Number::class.java.isAssignableFrom(type)
            || type.isPrimitive){
            printIndentation(file, isYaml, prefix)
            file.appendText("${field.name}$sep ?\n")
        } else if(type.isEnum){
            printIndentation(file, isYaml, prefix)
            file.appendText("${field.name}$sep ${type.enumConstants.joinToString(" | ")}\n")
        } else {
            if(!isYaml) {
                val tag = "$prefix.${field.name}"
                if (isCollection) {
                    file.appendText("# [[$tag]]\n")
                } else {
                    file.appendText("# [$tag]\n")
                }
                type.fields.forEach {
                    printObjectDefinition(false, file, tag, it)
                }
            } else {
                printIndentation(file, true, prefix)
                file.appendText("${field.name}$sep\n")
                type.fields.forEachIndexed { index, it ->
                    val p = if(index == 0 && isCollection) "  - " else "    "
                    printObjectDefinition(true, file, prefix+p, it)
                }
            }
        }
    }

    private fun printIndentation(file: File, isYaml: Boolean, prefix: String){
        if(isYaml)
            file.appendText("#${prefix}")
        else
            file.appendText("# ")
    }
}