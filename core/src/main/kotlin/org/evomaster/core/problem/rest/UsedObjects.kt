package org.evomaster.core.problem.rest

import org.evomaster.core.search.gene.*

class UsedObjects {
    /**
     * Mapping stores a link between the Action-parameter pair and the object being used as data.
     * The key is a pair of strings.
     * First string - the id of the RestCallAction being used (set in RestActionBuilder).
     * The id consists of Verb + path + unique identified (unique to the RestIndividual).
     * Second string - the name of the gene (as obtained by gene.getVariableName() )
     * Note! = the assumption is that there is only one variable of that name per action.
     * **/
    private val mapping:MutableMap<Pair<String, String> , Gene> = mutableMapOf()
    /**
     * Selection stores a link between the Action-parameter pair and the identifier of the field of the object being used as data.
     * The object itself can be found in the mapping map.
     * The key is a pair of strings.
     * First string - the id of the RestCallAction being used (set in RestActionBuilder).
     * The id consists of Verb + path + unique identified (unique to the RestIndividual).
     * Second string - the name of the gene (as obtained by gene.getVariableName() )
     * Note! = the assumption is that there is only one variable of that name per action.
     * **/
    private val selection:MutableMap<Pair<String, String>, Pair<String, String>> = mutableMapOf()
    private val select_body:MutableMap<String, Gene> = mutableMapOf()

    object GeneSpecialCases {
        /**
         * Some parameters require complete objects. Instead of the regular object-field pair, a complete object is returned.
         */
        const val COMPLETE_OBJECT: String = "Complete_object"
        /**
         * If a matching object cannot be found, for whatever reason, the process should continue. The gene is generated and mutated
         * as usual. It is not accompanied by any used objects, but is otherwise normal.
         */
        const val NOT_FOUND: String = "Not_found"
    }

    fun copy(): UsedObjects {
        val mapcopy: MutableMap<Pair<String, String> , Gene> = mutableMapOf()
        val selcopy: MutableMap<Pair<String, String>, Pair<String, String>> = mutableMapOf()
        val bodycopy: MutableMap<String, Gene> = mutableMapOf()
        mapping.forEach { k, v -> mapcopy[k] = v.copy() }
        selection.forEach{ k, v ->  selcopy[k] = v.copy() }
        select_body.forEach { k, v -> bodycopy[k] = v }

        val obj = UsedObjects()
        obj.mapping.putAll(mapcopy)
        obj.selection.putAll(selcopy)
        obj.select_body.putAll(bodycopy)

        return obj
    }

    fun usedObjects(): List<Gene>{
        return mapping.keys.flatMap { (mapping[it] as ObjectGene).fields.filter { fi -> fi.name == selection[it]!!.second }  }
    }

    fun assign(key:Pair<RestCallAction, Gene>, value:Gene, selectedField:Pair<String, String>){
        mapping[Pair(key.first.id, key.second.getVariableName())] = value
        selection[Pair(key.first.id, key.second.getVariableName())] = selectedField
    }

    fun selectbody(action:RestCallAction, obj:Gene){
        select_body[action.id] = obj
    }

    fun clear(){
        mapping.clear()
        selection.clear()
        select_body.clear()
    }

    fun getRelevantGene(action: RestCallAction, gene: Gene): Gene{
        val selectedField = selection[Pair(action.id, gene.getVariableName())]!!

        val retGene = when (selectedField.second) {
            GeneSpecialCases.COMPLETE_OBJECT -> mapping[Pair(action.id, gene.getVariableName())]!!
            else -> (mapping[Pair(action.id, gene.getVariableName())] as ObjectGene).fields
                    .filter { it.name === selectedField.second }
                    .first()
        }
        return retGene
    }
    fun isEmpty(): Boolean{
        return mapping.isEmpty()
    }
    fun exists(action: RestCallAction): Boolean{
        return mapping.keys.map { it.first }.contains(action.id)
    }
    fun allExist(actions: MutableList<RestAction>): Boolean {
        val restActions = actions.filter{ it::class == RestCallAction::class}
        return mapping.keys.map { it.first }.containsAll(restActions.map { (it as RestCallAction).id })
    }

    fun notCoveredActions(actions: MutableList<RestAction>): MutableList<RestCallAction> {
        val restActions = actions.filter{ it::class == RestCallAction::class}.map { (it as RestCallAction) }.toMutableList()
        return restActions.filterNot { exists(it) }.toMutableList()
    }

    fun coveredActions(): List<out String> {
        return mapping.keys.map { it.first }
    }
    fun getAllRelevantObjects(action: RestCallAction): List<Gene>{

        //Can an argument be made that even object from other verbs may be relevant
        //e.g. a GET for an obj with a previous POST that fails may indicate a problem?
        //return mapping.filter{ it.key.first == action.id}.map{ it.value }
        return mapping.map{it.value}
    }
    fun getObjectsForAction(action: RestCallAction): List<Gene>{
        return mapping.filter{it.key.first == action.id}.map { it.value }
    }

}
