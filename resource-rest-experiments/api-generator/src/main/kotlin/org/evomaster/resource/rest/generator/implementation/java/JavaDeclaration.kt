package org.evomaster.resource.rest.generator.implementation.java

import org.evomaster.resource.rest.generator.model.PropertySpecification
import org.evomaster.resource.rest.generator.template.DeclarationScript
import org.evomaster.resource.rest.generator.template.RegisterType

/**
 * created by manzh on 2019-08-13
 */

abstract class JavaDeclaration (val specification : PropertySpecification) : DeclarationScript, JavaTemplate{

    override fun getType(): String = specification.type

    override fun getDefaultValue(): String? = specification.defaultValue

    override fun getName(): String = specification.name

    override fun generateAsVarOfConstructor(types : RegisterType): String = "${types.getType(getType())} ${generateDefaultVarName()}"

    override fun generateSetterStatement(varName: String): String = "this.${getName()} = $varName"

    override fun generate(types : RegisterType): String {
        val content = StringBuilder()
        getTags().forEach {
            content.append("@$it")
            content.append(System.lineSeparator())
        }
        content.append("${formatBoundary(getBoundary())} ${if(isFinal()) "${getFinal()} " else ""}${if(isStatic()) "${getStatic()} " else ""}${types.getType(getType())} ${getName()} ${if (getDefaultValue().isNullOrBlank()) "" else "= ${getDefaultValue()}"} ${statementEnd()}")
        return content.toString()
    }
}