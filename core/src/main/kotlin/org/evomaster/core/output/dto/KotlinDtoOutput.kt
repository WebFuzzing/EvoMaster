package org.evomaster.core.output.dto

import org.evomaster.core.output.Lines
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.output.TestSuiteFileName
import java.nio.file.Path

class KotlinDtoOutput: JvmDtoOutput() {

    override fun writeClass(outputFormat: OutputFormat, testSuitePath: Path, testSuitePackage: String, dtoClass: DtoClass) {
        val dtoFilename = TestSuiteFileName(appendDtoPackage(dtoClass.name))
        val lines = Lines(outputFormat)
        setPackage(lines, testSuitePackage)
        addImports(lines)
        declareDtoClass(lines, dtoFilename.getClassName(), dtoClass)
        saveToDisk(lines.toString(), getTestSuitePath(testSuitePath, dtoFilename, outputFormat))
    }

    override fun getNewObjectStatement(dtoName: String, dtoVarName: String): String {
        return "val $dtoVarName = $dtoName()"
    }

    override fun getSetterStatement(dtoVarName: String, attributeName: String, value: String): String {
        return "$dtoVarName.${attributeName} = $value"
    }

    override fun getNewListStatement(listType: String, listVarName: String): String {
        return "val $listVarName = mutableListOf<$listType>()"
    }

    override fun getAddElementToListStatement(listVarName: String, value: String): String {
        return "$listVarName.add($value)"
    }

    private fun declareDtoClass(lines: Lines, dtoFilename: String, dtoClass: DtoClass) {
        lines.add("@JsonInclude(JsonInclude.Include.NON_NULL)")
        lines.add("class $dtoFilename(")
        addVariables(lines, dtoClass)
        lines.add(")")
    }

    private fun addVariables(lines: Lines, dtoClass: DtoClass) {
        dtoClass.fields.forEach {
            lines.indented {
                lines.add("@JsonProperty(\"${it.name}\")")
                lines.add("var ${it.name}: ${it.type}? = null,")
            }
            lines.addEmpty()
        }
    }

}
