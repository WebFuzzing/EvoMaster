package org.evomaster.resource.rest.generator.implementation.java

import com.google.googlejavaformat.java.Formatter
import org.evomaster.resource.rest.generator.model.ClazzSpecification
import org.evomaster.resource.rest.generator.template.*
import java.time.LocalDate

/**
 * created by manzh on 2019-08-13
 */

abstract class JavaClass<T> (val specification: T): ClassTemplate, JavaTemplate where T : ClazzSpecification {

    override fun isAbstract(): Boolean = false

    override fun getOutputSrcFolder(): String = specification.outputFolder

    override fun getOutputResourceFolder(): String = specification.resourceFolder

    override fun getFileSuffix(): String = "java"

    override fun getPackage(): String  = specification.rootPackage

    override fun getFileName(): String = getName()

    override fun getComments(): List<String> = listOf(
            """
                /**
                * automatically created on ${LocalDate.now()}
                */ 
            """.trimIndent()
    )
    override fun generateHeading(types: RegisterType)
        :String = """
                ${formatClassHeading(getBoundary(), getType(), isAbstract(), getName(), getImplementedInterface(), if (getSuperClazz().isEmpty()) "" else if(getSuperClazz().size == 1) generateSuperClazz(types)[0] else throw IllegalArgumentException("not allowed to have more than one supper classes in java."))}
        """.trimIndent()

    override fun generateEnding(types: RegisterType) : String  = clazzEnd()

    override fun includeDeclaration(): Boolean = true

    override fun generateConstructors(types: RegisterType): List<String> {
        if (getType() != ClassType.CLAZZ) return listOf()

        return listOf(
                """
                ${formatBoundary(getBoundary())} ${getName()} ${GeneralSymbol.LEFT_PARENTHESIS} ${GeneralSymbol.RIGHT_PARENTHESIS}${GeneralSymbol.LEFT_BRACE} ${GeneralSymbol.RIGHT_BRACE}
            """.trimIndent(),
                if (getDeclaration().isEmpty()) ""
                else
                    """
                    ${formatBoundary(getBoundary())} ${getName()} ${GeneralSymbol.LEFT_PARENTHESIS} ${getDeclaration().map { it.generateAsVarOfConstructor(types) }.joinToString(GeneralSymbol.COMMA)} ${GeneralSymbol.RIGHT_PARENTHESIS} ${GeneralSymbol.LEFT_BRACE} 
                        ${getDeclaration().map {d->
                        "${d.generateSetterStatement(d.generateDefaultVarName())} ${GeneralSymbol.SEMICOLON}"
                    }.joinToString(System.lineSeparator())}
                    ${GeneralSymbol.RIGHT_BRACE}        
                """.trimIndent())
    }


    override fun getBoundary(): Boundary = Boundary.PUBLIC

    override fun generateImports() : List<String> = getImports().map { formatImport(it) }

    override fun generatePackage(): String = formatPackage(getPackage())

    override fun formatScript(content: String): String {
        //return content;
        return Formatter().formatSource(content)
    }

}