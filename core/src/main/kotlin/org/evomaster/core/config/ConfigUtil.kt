package org.evomaster.core.config

import com.fasterxml.jackson.dataformat.toml.TomlMapper
import kotlin.io.path.Path
import kotlin.io.path.exists

object ConfigUtil {

    fun readFromToml(stringPath: String) : ConfigsFromFile{

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
    }
}