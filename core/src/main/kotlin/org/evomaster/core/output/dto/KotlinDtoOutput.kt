package org.evomaster.core.output.dto

import org.evomaster.core.output.Lines
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.output.TestSuiteFileName
import java.nio.file.Path

class KotlinDtoOutput: JvmDtoOutput() {

    override fun writeClass(testSuitePath: Path, testSuitePackage: String, outputFormat: OutputFormat, dtoClass: DtoClass) {
        val dtoFilename = TestSuiteFileName(appendDtoPackage(dtoClass.name))
        val lines = Lines(outputFormat)
        setPackage(lines, testSuitePackage)
        addImports(lines)
        declareClass(lines, dtoFilename.getClassName(), dtoClass)
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

    override fun getAddElementToAdditionalPropertiesStatement(additionalPropertiesVarName: String, key: String, value: String): String {
        return "$additionalPropertiesVarName.addAdditionalProperty($key, $value)"
    }

    private fun declareClass(lines: Lines, dtoFilename: String, dtoClass: DtoClass) {
        lines.add("@JsonInclude(JsonInclude.Include.NON_NULL)")
        lines.add("class $dtoFilename {")
        addVariables(lines, dtoClass)
        lines.add("}")
    }

    private fun addVariables(lines: Lines, dtoClass: DtoClass) {
//        dtoClass.fields.forEach {
        dtoClass.fieldsMap.forEach {
            lines.indented {
                lines.add("@JsonProperty(\"${it.key}\")")
                lines.add("var ${it.key}: ${it.value.type}? = null")
            }
            lines.addEmpty()
        }
        if (dtoClass.hasAdditionalProperties) {
            lines.indented {
                lines.add("@JsonIgnore")
//                lines.add("val additionalProperties: MutableMap<String, ${dtoClass.name}_ap> = mutableMapOf<String, ${dtoClass.name}_ap>()")
                lines.add("private val additionalProperties: MutableMap<String, ${dtoClass.additionalPropertiesDtoName}> = mutableMapOf()")
                lines.addEmpty()
                lines.add("@JsonAnyGetter")
                lines.add("fun getAdditionalProperties(): MutableMap<String, ${dtoClass.additionalPropertiesDtoName}> {")
                lines.indented {
                    lines.add("return additionalProperties")
                }
                lines.add("}")
                lines.addEmpty()
                lines.add("@JsonAnySetter")
                lines.add("fun addAdditionalProperty(name: String, value: ${dtoClass.additionalPropertiesDtoName}) {")
                lines.indented {
                    lines.add("additionalProperties[name] = value")
                }
                lines.add("}")
                lines.addEmpty()
            }
        }
    }

}
