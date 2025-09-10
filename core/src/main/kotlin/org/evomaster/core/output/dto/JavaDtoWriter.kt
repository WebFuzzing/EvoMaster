package org.evomaster.core.output.dto

import org.evomaster.core.output.Lines
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.output.TestSuiteFileName
import org.evomaster.core.utils.StringUtils
import java.nio.file.Files
import java.nio.file.Path

/**
 * When [EMConfig.dtoForRequestPayload] is enabled and [OutputFormat] is set to Java, this writer object will
 * create DTO java classes in the filesystem under the `dto` package. These DTO classes will then be used for
 * test case writing for JSON request payloads. Instead of having a stringified view of the payload, EM will
 * leverage these DTOs.
 */
object JavaDtoWriter {

    /**
     * @param testSuitePath under which the java class must be written
     * @param outputFormat forwarded to the [Lines] helper class and for setting the .java extension in the generated file
     * @param dtoClass to be written to filesystem
     */
    fun write(testSuitePath: Path, outputFormat: OutputFormat, dtoClass: DtoClass) {
        val dtoFilename = TestSuiteFileName(appendDtoPackage(dtoClass.name))
        val lines = Lines(outputFormat)
        setPackage(lines)
        addImports(lines)
        initClass(lines, dtoFilename.getClassName())
        addClassContent(lines, dtoClass)
        closeClass(lines)
        saveToDisk(lines.toString(), getTestSuitePath(testSuitePath, dtoFilename, outputFormat))
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

    private fun initClass(lines: Lines, dtoFilename: String) {
        lines.add("@JsonInclude(JsonInclude.Include.NON_NULL)")
        lines.add("public class $dtoFilename {")
        lines.addEmpty()
    }

    private fun addClassContent(lines: Lines, dtoClass: DtoClass) {
        addVariables(lines, dtoClass)
        addGettersAndSetters(lines, dtoClass)
    }

    private fun addVariables(lines: Lines, dtoClass: DtoClass) {
        dtoClass.fields.forEach {
            lines.indented {
                lines.add("@JsonProperty(\"${it.name}\")")
                lines.add("private Optional<${it.type}> ${it.name};")
            }
            lines.addEmpty()
        }
    }

    private fun addGettersAndSetters(lines: Lines, dtoClass: DtoClass) {
        dtoClass.fields.forEach {
            val varName = it.name
            val varType = it.type
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

    private fun getTestSuitePath(testSuitePath: Path, dtoFilename: TestSuiteFileName, outputFormat: OutputFormat) : Path{
        return testSuitePath.resolve(dtoFilename.getAsPath(outputFormat))
    }

    private fun saveToDisk(testFileContent: String, path: Path) {
        Files.createDirectories(path.parent)
        Files.deleteIfExists(path)
        Files.createFile(path)

        path.toFile().appendText(testFileContent)
    }
}
