package org.evomaster.core.config

import com.fasterxml.jackson.dataformat.toml.TomlMapper
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

        val mapper = TomlMapper()
        val cff = try{
            mapper.readValue(path.toFile(), ConfigsFromFile::class.java)
        } catch (e: Exception){
            throw IllegalArgumentException("Failed to parse TOML config file at: ${path.toAbsolutePath()}", e)
        }

        //TODO validate here? in TOML there is no schema yet

        return cff;
    }

    fun createConfigFileTemplateToml(stringPath: String, template: ConfigsFromFile) {

        val path = Path(stringPath)
        if(path.exists()){
            throw IllegalArgumentException("Config file already exists at: ${path.toAbsolutePath()}")
        }
        if(!stringPath.endsWith(".toml")){
            throw IllegalArgumentException("Configuration file name does not end in '.toml': $stringPath")
        }

        val file = path.toFile()
        file.parentFile.mkdirs()
        file.createNewFile()

        file.appendText("### Template configuration file for EvoMaster.\n")
        file.appendText("### Most important parameters are already present here, commented out.\n")
        file.appendText("### Note that there are more parameters that can be configured. For a full list, see:\n")
        file.appendText("### https://github.com/EMResearch/EvoMaster/blob/master/docs/options.md\n")
        file.appendText("### or check them with the --help option.\n")
        file.appendText("\n")
        file.appendText("\n")
        file.appendText("[configs]\n")
        template.configs
            .toSortedMap()
            .forEach {
            file.appendText("# ${it.key}=${it.value}\n")
        }

        file.appendText("\n\n\n")
        file.appendText("### Authentication configurations.\n")
        file.appendText("### For each possible registered user, can provide an ${AuthenticationDto::class.simpleName}" +
                " object to define how to log them in.\n")
        file.appendText("### Different types of authentication mechanisms can be configured here.\n")
        file.appendText("### For more information, read: https://github.com/EMResearch/EvoMaster/blob/master/docs/auth.md\n")
        file.appendText("\n")

        val auth = "auth"

        AuthenticationDto::class.java.fields
            .filter { it.name != "name" }
            .forEach {
                file.appendText("# [[$auth]]\n")
                file.appendText("# name=?\n")
                printObjectDefinitionToml(file,auth,it)
                file.appendText("\n")
            }


        file.appendText("\n")
    }

    private fun printObjectDefinitionToml(file: File, prefix: String, field: Field){

        var isCollection = false

        val type = if(List::class.java.isAssignableFrom(field.type)
            || Set::class.java.isAssignableFrom(field.type)
            || field.type.isArray){
            isCollection = true
            (field.genericType as ParameterizedType).actualTypeArguments[0] as Class<*>
        } else {
            field.type
        }

        if(Boolean::class.java.isAssignableFrom(type) || java.lang.Boolean.TYPE == type){
            file.appendText("# ${field.name}= true | false\n")
        } else if(String::class.java.isAssignableFrom(type)
            || Number::class.java.isAssignableFrom(type)
            || type.isPrimitive){
            file.appendText("# ${field.name}=?\n")
        } else if(type.isEnum){
            file.appendText("# ${field.name}= ${type.enumConstants.joinToString(" | ")}\n")
        } else {
            val tag = "$prefix.${field.name}"
            if(isCollection){
                file.appendText("# [[$tag]]\n")
            } else {
                file.appendText("# [$tag]\n")
            }
            type.fields.forEach {
                printObjectDefinitionToml(file, tag, it)
            }
        }
    }
}