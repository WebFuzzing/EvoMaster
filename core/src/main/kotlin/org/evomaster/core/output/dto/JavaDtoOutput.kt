package org.evomaster.core.output.dto

import org.evomaster.core.output.Lines
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.output.TestSuiteFileName
import java.nio.file.Path

class JavaDtoOutput: JvmDtoOutput() {

    override fun writeClass(outputFormat: OutputFormat, testSuitePath: Path, testSuitePackage: String, dtoClass: DtoClass) {
        val dtoFilename = TestSuiteFileName(appendDtoPackage(dtoClass.name))
        val lines = Lines(outputFormat)
        setPackage(lines, testSuitePackage)
        addImports(lines)
        initClass(lines, dtoFilename.getClassName())
        addClassContent(lines, dtoClass)
        closeClass(lines)
        saveToDisk(lines.toString(), getTestSuitePath(testSuitePath, dtoFilename, outputFormat))
    }

    override fun getNewObjectStatement(dtoName: String, dtoVarName: String): String {
        return "$dtoName $dtoVarName = new $dtoName();"
    }

    override fun getSetterStatement(dtoVarName: String, attributeName: String, value: String): String {
        return "$dtoVarName.set${capitalizeFirstChar(attributeName)}($value);"
    }

    override fun getNewListStatement(listType: String, listVarName: String): String {
        return "List<$listType> $listVarName = new ArrayList<$listType>();"
    }

    override fun getAddElementToListStatement(listVarName: String, value: String): String {
        return "$listVarName.add($value);"
    }

    override fun getAddElementToAdditionalPropertiesStatement(additionalPropertiesVarName: String, key: String, value: String): String {
        return "$additionalPropertiesVarName.addAdditionalProperty($key, $value);"
    }

    private fun initClass(lines: Lines, dtoFilename: String) {
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
        dtoClass.fieldsMap.forEach {
            lines.indented {
                lines.add("@JsonProperty(\"${it.key}\")")
                lines.add("private Optional<${it.value.type}> ${it.key};")
            }
            lines.addEmpty()
        }
        if (dtoClass.hasAdditionalProperties) {
            lines.indented {
                lines.add("@JsonIgnore")
                lines.add("private Map<String, ${dtoClass.additionalPropertiesDtoName}> additionalProperties = new HashMap<>();")
            }
            lines.addEmpty()
        }
    }

    private fun addGettersAndSetters(lines: Lines, dtoClass: DtoClass) {
        dtoClass.fieldsMap.forEach {
            val varName = it.key
            val varType = it.value.type
            val capitalizedVarName = capitalizeFirstChar(varName)
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
        if (dtoClass.hasAdditionalProperties) {
            lines.indented {
                lines.add("@JsonAnyGetter")
                lines.add("public Map<String, ${dtoClass.additionalPropertiesDtoName}> getAdditionalProperties() {")
                lines.indented {
                    lines.add("return additionalProperties;")
                }
                lines.add("}")
                lines.addEmpty()
                lines.add("@JsonAnySetter")
                lines.add("public void addAdditionalProperty(String name, ${dtoClass.additionalPropertiesDtoName} value) {")
                lines.indented {
                    lines.add("this.additionalProperties.put(name, value);")
                }
                lines.add("}")
            }
            lines.addEmpty()
        }
    }

}
