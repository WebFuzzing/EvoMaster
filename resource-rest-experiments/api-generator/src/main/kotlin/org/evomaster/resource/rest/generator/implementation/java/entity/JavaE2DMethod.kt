package org.evomaster.resource.rest.generator.implementation.java.entity

import org.evomaster.resource.rest.generator.implementation.java.JavaMethod
import org.evomaster.resource.rest.generator.model.EntityClazz
import org.evomaster.resource.rest.generator.model.ResNodeTypedPropertySpecification
import org.evomaster.resource.rest.generator.template.Boundary


/**
 * created by manzh on 2019-08-16
 *
 * convert entity to dto
 */
class JavaE2DMethod(val specification: EntityClazz) : JavaMethod() {

    override fun getParams(): Map<String, String> = mapOf()

    override fun getBody(): List<String> {
        val content = mutableListOf<String>()

        val dtoClazzName = getReturn()
        val dtoVar = "dto"

        content.add("${formatInstanceClassAndAssigned(dtoClazzName, dtoVar, listOf())}")

        //id
        content.add(assignValue("$dtoVar.${specification.dto.idProperty.name}", "this.${specification.idProperty.nameGetterName()}()"))

        //default properties
        (0 until specification.defaultProperties.size).forEach {index->
            content.add(assignValue("$dtoVar.${specification.dto.defaultProperties[index].name}", "this.${specification.defaultProperties[index].nameGetterName()}()"))
        }

        //refer node
        (0 until specification.referToOthers.size).forEach { index->
            val id =
                    if (specification.referToOthers[index] is ResNodeTypedPropertySpecification)
                        (specification.referToOthers[index] as ResNodeTypedPropertySpecification).itsIdProperty
                    else throw IllegalArgumentException("${specification.referToOthers[index].name} is not ResNodeTypedPropertySpecification")
            content.add(assignValue("$dtoVar.${specification.dto.referToOthers[index].name}", "this.${specification.referToOthers[index].nameGetterName()}().${id.nameGetterName()}()"))
        }

        //owned node
        (0 until specification.ownOthers.size).forEach { index->
            val id =
                    if (specification.ownOthers[index] is ResNodeTypedPropertySpecification)
                        (specification.ownOthers[index] as ResNodeTypedPropertySpecification).itsIdProperty
                    else throw IllegalArgumentException("${specification.ownOthers[index].name} is not ResNodeTypedPropertySpecification")
            content.add(assignValue("$dtoVar.${specification.dto.ownOthers[index].name}", "this.${specification.ownOthers[index].nameGetterName()}().${id.nameGetterName()}()"))

            val ps = specification.dto.ownOthersProperties[index]
            ps.forEach {p->
                val op = (p as? ResNodeTypedPropertySpecification)?:throw IllegalArgumentException("${specification.ownOthers[index].name} is not ResNodeTypedPropertySpecification")
                content.add(assignValue("$dtoVar.${op.name}", "this.${specification.ownOthers[index].nameGetterName()}().${op.itsIdProperty.nameGetterName()}()"))
            }

        }
        content.add("return dto;")
        return content
    }

    override fun getReturn(): String = specification.getDto?.returnType?: throw IllegalArgumentException("getDto of ${specification.name} is null")

    override fun getName(): String = specification.getDto?.name?: throw IllegalArgumentException("getDto of ${specification.name} is null")

    override fun getBoundary(): Boundary = Boundary.PUBLIC

    fun getInvocation(obj: String?): String  = getInvocation(obj, listOf())

    private fun assignValue(target : String, current: String)
    = """
        $target = $current;
    """.trimIndent()
}