package org.evomaster.resource.rest.generator.implementation.java

import org.evomaster.resource.rest.generator.template.Boundary
import org.evomaster.resource.rest.generator.template.ClassType
import org.evomaster.resource.rest.generator.template.GeneralSymbol
import java.lang.IllegalArgumentException

/**
 * created by manzh on 2019-08-14
 */
interface JavaTemplate {

    fun formatBoundary(boundary : Boundary)
            : String = if (boundary!= Boundary.NONE) boundary.name.toLowerCase() else throw IllegalArgumentException("invalid setting of the boundary: $boundary.name}")

    fun formatMethodHeading(boundary: Boundary, isStatic : Boolean, returnType : String?, methodName : String, paramPair : Map<String, String>, paramTag : Map<String, String>)
            : String = "${formatBoundary(boundary)} ${if (isStatic) "static " else ""}${returnType?:"void"} $methodName ${GeneralSymbol.LEFT_PARENTHESIS} ${paramPair.map { "${if (paramTag.containsKey(it.key)) "@${paramTag[it.key]} " else ""}${it.value} ${it.key}" }.joinToString(GeneralSymbol.COMMA)} ${GeneralSymbol.RIGHT_PARENTHESIS} ${GeneralSymbol.LEFT_BRACE}"

    fun formatMethodEnding() = GeneralSymbol.RIGHT_BRACE

    fun statementEnd() = GeneralSymbol.SEMICOLON

    fun clazzStart() = GeneralSymbol.LEFT_BRACE
    fun clazzEnd() = GeneralSymbol.RIGHT_BRACE

    fun methodStart() = GeneralSymbol.RIGHT_BRACE
    fun methodEnd() = GeneralSymbol.RIGHT_BRACE


    fun formatClassHeading(boundary: Boundary, type: ClassType, isAbstract: Boolean, clazzName : String, implementInterface : List<String> = listOf(), supperClazz : String = "")
            = "${formatBoundary(boundary)}${if (isAbstract) " abstract " else " "}${formatClazzType(type)} $clazzName${if (supperClazz.isNullOrBlank()) " " else " extends $supperClazz"}${if (implementInterface.isEmpty()) " " else " implements ${implementInterface.joinToString(",")}"} ${clazzStart()}"

    fun formatPackage(path : String) = "package $path ${statementEnd()}"

    fun formatImport(path : String) = "import $path ${statementEnd()}"

    fun formatClazzType(type: ClassType) : String{
        return  when(type){
            ClassType.CLAZZ -> "class"
            ClassType.ENUM -> "enum"
            ClassType.INTERFACE -> "interface"
        }
    }

    fun formatInstanceClassAndAssigned(type : String, paramName : String, consParams : List<String>)= "$type $paramName = new $type(${consParams.joinToString(GeneralSymbol.COMMA)});"

    fun formatInstanceClass(type : String, consParams : List<String>)= "new $type(${consParams.joinToString(GeneralSymbol.COMMA)})"

    fun formatMethodInvocation(obj : String?, methodName: String, params : List<String>) = "${if (obj.isNullOrBlank())"" else "$obj."}$methodName(${params.joinToString(GeneralSymbol.COMMA)})"

}