package org.evomaster.core.output

import io.swagger.v3.oas.models.OpenAPI
import org.evomaster.core.problem.rest.RestActionBuilderV3
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.search.gene.optional.CustomMutationRateGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.optional.OptionalGene

class ObjectGenerator {

    private lateinit var swagger: OpenAPI
    private val modelCluster: MutableMap<String, ObjectGene> = mutableMapOf()

    fun setSwagger(sw: OpenAPI,
                   enableConstraintHandling: Boolean){
        swagger = sw
        modelCluster.clear()
        RestActionBuilderV3.getModelsFromSwagger(swagger, modelCluster, RestActionBuilderV3.Options(enableConstraintHandling=enableConstraintHandling))
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
            CustomMutationRateGene::class -> return (foundGene as CustomMutationRateGene<*>).gene
            else -> return foundGene
        }
    }

    fun addResponseObjects(action: RestCallAction, individual: RestIndividual){
        val refs = action.responseRefs.values
        refs.forEach{
            val respObject = swagger.components.schemas.get(it)

        }
    }

    /**
     * [getNamedReference] returns the ObjectGene with the matching name from the model cluster.
     * It is meant to be used in conjunction with [containsKey], to check if the key is present and define appropriate
     * behaviour if it is not present.
     * If [getNamedReference] is called with a string key that is not contained in the model cluster, the default
     * behaviour is to return an empty ObjectGene with the name "NotFound".
     */
    fun getNamedReference(name: String): ObjectGene{
        return when{
            containsKey(name) -> modelCluster.getValue(name)
            else -> ObjectGene("NotFound", listOf())
        }
    }
    fun containsKey(name: String): Boolean{
        return modelCluster.containsKey(name)
    }
}