package org.evomaster.core.output

import io.swagger.v3.oas.models.OpenAPI
import org.evomaster.core.problem.rest.RestActionBuilderV3
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.search.gene.DisruptiveGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.OptionalGene

class ObjectGenerator {

    private lateinit var swagger: OpenAPI
    private val modelCluster: MutableMap<String, ObjectGene> = mutableMapOf()

    fun initialize() {
        if (swagger != null) {
            modelCluster.clear()
            RestActionBuilderV3.getModelsFromSwagger(swagger, modelCluster)
        }
    }

    fun setSwagger(sw: OpenAPI){
        swagger = sw
        if (swagger != null) {
            modelCluster.clear()
            RestActionBuilderV3.getModelsFromSwagger(swagger, modelCluster)
        }
    }

    fun getSwagger(): OpenAPI{
        return swagger
    }

    fun longestCommonSubsequence(a: String, b: String): String {
        if (a.length > b.length) return longestCommonSubsequence(b, a)
        var res = ""
        for (ai in 0 until a.length) {
            for (len in a.length - ai downTo 1) {
                for (bi in 0 until b.length - len + 1) {
                    if (a.regionMatches(ai, b, bi,len) && len > res.length) {
                        res = a.substring(ai, ai + len)
                    }
                }
            }
        }
        return res
    }


    fun <T> likelyhoodsExtended(parameter: String, candidates: MutableMap<String, T>): MutableMap<Pair<String, String>, Float>{
        //TODO BMR: account for empty candidate sets.
        val result = mutableMapOf<Pair<String, String>, Float>()
        var sum : Float = 0.toFloat()

        if (candidates.isEmpty()) return result

        candidates.forEach { k, v ->
            for (field in (v as ObjectGene).fields) {
                val fieldName = field.name
                val extendedName = "$k${fieldName}"
                // even if there are not characters in common, a matching field should still have
                // a probability of being chosen (albeit a small one).
                val temp = (1.toLong() + longestCommonSubsequence(parameter.toLowerCase(), extendedName.toLowerCase())
                        .length.toFloat())/ (1.toLong() + Integer.max(parameter.length, extendedName.length).toFloat())
                result[Pair(k, fieldName)] = temp
                sum += temp
            }
        }
        result.forEach { k, u ->
            result[k] = u/sum
        }

        return result
    }

    private fun findSelectedGene(selectedGene: Pair<String, String>): Gene {

        val foundGene = (modelCluster[selectedGene.first]!!).fields.filter{ field ->
            field.name == selectedGene.second
        }.first()
        when (foundGene::class) {
            OptionalGene::class -> return (foundGene as OptionalGene).gene
            DisruptiveGene::class -> return (foundGene as DisruptiveGene<*>).gene
            else -> return foundGene
        }
    }

    fun addResponseObjects(action: RestCallAction, individual: RestIndividual){
        val refs = action.responseRefs.values
        refs.forEach{
            val respObject = swagger.components.schemas.get(it)

        }
    }

    fun getNamedReference(name: String): ObjectGene{
        return modelCluster.getValue(name)
    }

}