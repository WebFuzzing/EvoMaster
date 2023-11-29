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
}