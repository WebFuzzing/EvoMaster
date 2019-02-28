package org.evomaster.core.problem.rest

import org.evomaster.core.search.gene.*

class UsedObjs {
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
    fun copy(): UsedObjs {
        val mapcopy: MutableMap<Pair<String, String> , Gene> = mutableMapOf()
        val selcopy: MutableMap<Pair<String, String>, Pair<String, String>> = mutableMapOf()
        val bodycopy: MutableMap<String, Gene> = mutableMapOf()
        mapping.forEach { k, v -> mapcopy[k] = v.copy() }
        selection.forEach{ k, v ->  selcopy[k] = v.copy() }
        select_body.forEach { k, v -> bodycopy[k] = v }

        val obj = UsedObjs()
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

    fun clearLists(){
        mapping.clear()
        selection.clear()
        select_body.clear()
    }

    fun getRelevantGene(action: RestCallAction, gene: Gene): Gene{
        val selectedField = selection[Pair(action.id, gene.getVariableName())]!!

        val retGene = when (selectedField.second) {
            "Complete_object" -> mapping[Pair(action.id, gene.getVariableName())]!!
            else -> (mapping[Pair(action.id, gene.getVariableName())] as ObjectGene).fields
                    .filter { it.name === selectedField?.second }
                    .first()
        }
        return retGene
    }
    fun isEmpty(): Boolean{
        return mapping.isEmpty()
    }
}
