package org.evomaster.core.problem.rest.schema

import org.evomaster.core.problem.rest.arazzo.parser.ArazzoParser
import org.evomaster.core.remote.SutProblemException
import org.slf4j.LoggerFactory
import java.net.URI
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.readText
import kotlin.jvm.java
import kotlin.text.startsWith

object ArazzoAccess {

    private val log = LoggerFactory.getLogger(ArazzoAccess::class.java)

    fun parseArazzo(schemaText: String, sourceLocation: SchemaLocation): SchemaArazzo {

        val schemaParsed = ArazzoParser.parseSchemaText(schemaText)
        return SchemaArazzo(schemaText, schemaParsed.first, schemaParsed.second, sourceLocation)

    }

    fun getArazzoFromLocation(
        arazzoLocation: String
    ): SchemaArazzo {

        //could be either JSON or YAML
        val data: String
        val location: SchemaLocation

        data = readFromDisk(arazzoLocation);
        location = SchemaLocation(arazzoLocation, SchemaLocationType.LOCAL)

        return parseArazzo(data, location);
    }

    private fun readFromDisk(arazzoLocation: String) : String {
        // file schema
        val fileScheme = "file:"

        // create paths
        val path = try {
            if (arazzoLocation.startsWith(fileScheme, true)) {
                Paths.get(URI.create(arazzoLocation))
            }
            else {
                Paths.get(arazzoLocation)
            }
        }
        // Exception is thrown if the path is not valid
        catch (e: Exception) {
            // state the exception with the error message
            throw SutProblemException(
                "The file path provided for the Arazzo Schema $arazzoLocation" +
                        " ended up with the following error: " + e.message
            )
        }

        // If the path is valid but the file does not exist, an exception is thrown
        if (!Files.exists(path)) {
            throw SutProblemException("The provided Arazzo file does not exist: $arazzoLocation")
        }

        // return the schema text
        return path.toFile().readText()
    }

}