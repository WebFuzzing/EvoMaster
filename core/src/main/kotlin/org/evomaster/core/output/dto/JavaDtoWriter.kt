package org.evomaster.core.output.dto

import io.swagger.v3.oas.models.media.BooleanSchema
import io.swagger.v3.oas.models.media.IntegerSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import org.evomaster.core.output.Lines
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.output.TestSuiteFileName
import org.evomaster.core.utils.StringUtils
import java.nio.file.Files
import java.nio.file.Path

class JavaDtoWriter(private val testSuitePath: Path,
                    private val outputFormat: OutputFormat,
                    name: String,
                    private val schema: Schema<*>) {

    val dtoFilename: TestSuiteFileName

    init {
        dtoFilename = TestSuiteFileName(appendDtoPackage(name))
    }

    fun write() {
        val lines = Lines(outputFormat)
        setPackage(lines)
        addImports(lines)
        initClass(lines)
        addClassContent(lines)
        closeClass(lines)
        saveToDisk(lines.toString(), getTestSuitePath())
    }

    private fun setPackage(lines: Lines) {
        lines.add("package dto;")
        lines.addEmpty()
    }

    private fun addImports(lines: Lines) {
        lines.add("import java.util.Optional;")
        lines.addEmpty()
        lines.add("import com.fasterxml.jackson.annotation.JsonInclude;")
        lines.add("import shaded.com.fasterxml.jackson.annotation.JsonProperty;")
        lines.addEmpty()
    }

    private fun initClass(lines: Lines) {
        lines.add("@JsonInclude(JsonInclude.Include.NON_NULL)")
        lines.add("public class ${dtoFilename.getClassName()} {")
        lines.addEmpty()
    }

    private fun addClassContent(lines: Lines) {
        addVariables(lines)
        addGettersAndSetters(lines)
    }

    private fun addVariables(lines: Lines) {
        schema.properties.forEach {
            val varName = it.key
            val varType = when {
                it.value is StringSchema -> "String"
                it.value is BooleanSchema -> "Boolean"
                it.value is IntegerSchema -> "Integer"
                it.value is Schema -> extractRef(it.value.`$ref`)
                else -> throw IllegalStateException("Schema '${it.value.type}' unrecognized")
            }
            lines.indented {
                lines.add("@JsonProperty(\"${varName}\")")
                lines.add("private Optional<${varType}> ${varName};")
            }
            lines.addEmpty()
        }
    }

    private fun addGettersAndSetters(lines: Lines) {
        schema.properties.forEach {
            val varName = it.key
            val varType = when {
                it.value is StringSchema -> "String"
                it.value is BooleanSchema -> "Boolean"
                it.value is IntegerSchema -> "Integer"
                it.value is Schema -> extractRef(it.value.`$ref`)
                else -> throw IllegalStateException("Schema '${it.value.type}' unrecognized")
            }
            val capitalizedVarName = StringUtils.capitalization(varName)
            lines.indented {
                lines.add("public Optional<${varType}> get${capitalizedVarName}() {")
                lines.indented {
                    lines.add("return ${varName};")
                }
                lines.add("}")
                lines.addEmpty()
                lines.add("public void set${capitalizedVarName}(${varType} ${varName}) {")
                lines.indented {
                    lines.add("this.${varName} = Optional.ofNullable(${varName});")
                }
                lines.add("}")
            }
            lines.addEmpty()
        }
    }

    private fun closeClass(lines: Lines) {
        lines.add("}")
    }

    private fun appendDtoPackage(name: String): String {
        return "dto.$name"
    }

    private fun extractRef(ref: String): String {
        return ref.substring(ref.lastIndexOf("/")+1)
    }

    private fun getTestSuitePath() : Path{
        return testSuitePath.resolve(dtoFilename.getAsPath(outputFormat))
    }

    private fun saveToDisk(testFileContent: String, path: Path) {
        Files.createDirectories(path.parent)
        Files.deleteIfExists(path)
        Files.createFile(path)

        path.toFile().appendText(testFileContent)
    }
}
