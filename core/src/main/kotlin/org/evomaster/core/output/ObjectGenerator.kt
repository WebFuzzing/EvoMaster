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

    /*
    private fun proposeObject(g: Gene): Pair<ObjectGene, Pair<String, String>> {
        var restrictedModels = mutableMapOf<String, ObjectGene>()
        when (g::class) {
            ObjectGene::class -> {
                // If the gene is an object, select a suitable one from the Model CLuster (based on the Swagger)
                restrictedModels = modelCluster.filter{ model ->
                    model.value.fields
                            .map{ it.name }
                            .toSet().containsAll((g as ObjectGene).fields.map { it.name }) }.toMutableMap()
                if (restrictedModels.isEmpty()) return Pair(ObjectGene("none", mutableListOf()), Pair("none", UsedObjects.GeneSpecialCases.NOT_FOUND))
                val ret = randomness.choose(restrictedModels)
                return Pair(ret, Pair(ret.name, UsedObjects.GeneSpecialCases.COMPLETE_OBJECT))
            }
            else -> {
                modelCluster.forEach { k, model ->
                    val fields = model.fields.filter { field ->
                        when (field::class) {
                            OptionalGene::class -> g::class === (field as OptionalGene).gene::class
                            else -> g::class === field::class
                        }
                    }
                    restrictedModels[k] = ObjectGene(model.name, fields)
                }
            }
        }

        //BMR: Here I am trying to have the map sorted by value (which is the probability of choosing the respective pairing),
        // but it should still be a map (i.e. maintain the link between the "key" and the associated probability.
        val likely = likelyhoodsExtended(g.getVariableName(), restrictedModels).entries
                .sortedBy { -it.value }
                .associateBy({it.key}, {it.value})

        if (likely.isNotEmpty()){
            // there is at least one likely match
            val selected = pickObject((likely as MutableMap<Pair<String, String>, Float>), probabilistic = true)
            val selObject = modelCluster.get(selected.first)!!
            return Pair(selObject, selected)
        }
        else{
            // there is no likely match
            val fields = listOf(g)
            val wrapper = ObjectGene("Gene_wrapper_object", fields)
            return Pair(wrapper, Pair("", UsedObjects.GeneSpecialCases.NOT_FOUND))
        }
    }


     */

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

    /*
    fun pickObject(map: MutableMap<Pair<String, String>, Float>, probabilistic: Boolean = true ): Pair<String, String>{

        var found = map.keys.first()
        if (probabilistic) {
            //found = pickWithProbability(map)
            found = randomness.chooseByProbability(map)
        }
        return found
    }



    fun addMissingObjects(individual: RestIndividual){
        val missingActions = individual.usedObjects.notCoveredActions(individual.seeActions().filterIsInstance<RestCallAction>().toMutableList())
        if (missingActions.isEmpty()){
            return // no actions are missing.
        }
        else{
            missingActions.forEach { action ->
                addObjectsForAction(action, individual)
            }
        }
    }*/

    /**
     * When an individual has no objects attached (as is often the case) [addObjects] should create all relevant objects
     * and add them to the attached [usedObjects].
     * Note: I expect this will change, as I want the system to first check if a relevant object exists, and only then
     * create a new one if needed.
     *
     */
    /*
    fun addObjects(individual: RestIndividual){
        val actions = individual.seeActions().filterIsInstance<RestCallAction>()
        actions.forEach { action ->
            addObjectsForAction(action, individual)
        }
    }

     */

    /** Add objects for given Action
     * Some actions (e.g. Delete) can be added to a RestIndividual without randomization.
     * This function (together with the RestIndividual's ensureCoherence() can be used to add missing objects.
     * **/
    /*
    fun addObjectsForAction (action: RestCallAction, individual: RestIndividual) {
        action.seeGenes().forEach { g ->
            val innerGene = when (g::class){
                OptionalGene::class -> (g as OptionalGene).gene
                DisruptiveGene::class -> (g as DisruptiveGene<*>).gene
                else -> g
            }
            val (proposed, field) = proposeObject(innerGene)

            when(field.second) {
                UsedObjects.GeneSpecialCases.NOT_FOUND -> {
                    // In these cases, no object is created.
                }
                UsedObjects.GeneSpecialCases.COMPLETE_OBJECT -> {
                    if (innerGene.isMutable()) innerGene.randomize(randomness, true)

                    individual.usedObjects.assign(Pair(action, g), innerGene, field)
                    individual.usedObjects.selectbody(action, innerGene)
                }
                else -> {
                    proposed.randomize(randomness, true)
                    val proposedGene = findSelectedGene(field)

                    proposedGene.copyValueFrom(innerGene)
                    individual.usedObjects.assign(Pair(action, g), proposed, field)
                    individual.usedObjects.selectbody(action, proposed)
                }
            }
        }
    }

     */

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