package org.evomaster.core.output.dto

import org.evomaster.core.output.Lines
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.output.TestSuiteFileName
import org.evomaster.core.utils.StringUtils
import java.nio.file.Path

class JavaDtoOutput: JvmDtoOutput() {

    override fun writeClass(outputFormat: OutputFormat, testSuitePath: Path, testSuitePackage: String, dtoClass: DtoClass) {
        if (!outputFormat.isJava()) {

        }
        val dtoFilename = TestSuiteFileName(appendDtoPackage(dtoClass.name))
        val lines = Lines(outputFormat)
        setPackage(lines, testSuitePackage)
        addImports(lines)
        initDtoClass(lines, dtoFilename.getClassName())
        addClassContent(lines, dtoClass)
        closeClass(lines)
        saveToDisk(lines.toString(), getTestSuitePath(testSuitePath, dtoFilename, outputFormat))
    }

    override fun getNewObjectStatement(dtoName: String, dtoVarName: String): String {
        return "$dtoName $dtoVarName = new $dtoName();"
    }

    override fun getSetterStatement(dtoVarName: String, attributeName: String, value: String): String {
        return "$dtoVarName.set${StringUtils.capitalization(attributeName)}($value);"
    }

    override fun getNewListStatement(listType: String, listVarName: String): String {
        return "List<$listType> $listVarName = new ArrayList<$listType>();"
    }

    override fun getAddElementToListStatement(listVarName: String, value: String): String {
        return "$listVarName.add($value);"
    }

    private fun initDtoClass(lines: Lines, dtoFilename: String) {
        lines.add("@JsonInclude(JsonInclude.Include.NON_NULL)")
        lines.add("public class $dtoFilename {")
        lines.addEmpty()
    }

    private fun closeClass(lines: Lines) {
        lines.add("}")
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

}
