package org.evomaster.core.problem.rest

import org.evomaster.core.search.gene.*

class UsedObj(
              val mapping:MutableMap<Pair<String, Gene> , Gene> = mutableMapOf(),
              val selection:MutableMap<Pair<String, Gene>, Pair<String, String>> = mutableMapOf(),
              val select_body:MutableMap<String, Gene> = mutableMapOf()){

    fun copy(): UsedObj {
        val mapcopy: MutableMap<Pair<String, Gene> , Gene> = mutableMapOf()
        val selcopy: MutableMap<Pair<String, Gene>, Pair<String, String>> = mutableMapOf()
        val bodycopy: MutableMap<String, Gene> = mutableMapOf()
        mapping.forEach { k, v -> mapcopy[k] = v.copy() }
        selection.forEach{ k, v ->  selcopy[k] = v.copy() }
        select_body.forEach { k, v -> bodycopy[k] = v }

        return UsedObj(mapcopy, selcopy, bodycopy)
    }

    fun usedObjects(): List<Gene>{
        //return all objects for mutation and randomization purposes
        return mapping.values.flatMap{ it.flatView() }
    }

    fun coherenceCheck(){
        if (!mapping.isEmpty()){
            mapping.forEach { key, value ->
                when (value::class){
                    ObjectGene::class -> {
                        val sel = selection[key] as Pair<String, String>
                        val temp = when (sel.second) {
                            "Complete_object" -> OptionalGene("temp", (value as ObjectGene))
                            else -> (value as ObjectGene).fields.filter { g -> g.name == sel.second }?.single()
                        }
                        val v = when (temp::class){
                            OptionalGene::class -> temp
                            else -> OptionalGene("temp", temp)
                        }
                        when (key.second::class) {
                            DisruptiveGene::class -> (key.second as DisruptiveGene<*>).gene.copyValueFrom((v as OptionalGene).gene)
                            OptionalGene::class ->  (key.second as OptionalGene).gene.copyValueFrom((v as OptionalGene).gene)
                            ObjectGene::class -> (key.second as ObjectGene).copyValueFrom((v as OptionalGene).gene)
                            else -> key.second.copyValueFrom(v)
                        }
                    }
                }

            }
        }
    }

    fun assign(key:Pair<RestCallAction, Gene>, value:Gene, selectedField:Pair<String, String>){
        mapping[Pair(key.first.id, key.second)] = value
        selection[Pair(key.first.id, key.second)] = selectedField
    }

    fun selectbody(action:RestCallAction, obj:Gene){
        select_body[action.id] = obj
    }


    fun displayInline() : String{
        //display inline (mostly for debugging)
        var res = "{{"
        mapping.forEach { k, v ->
            res += "[${k.first}, ${k.second}]=${v.getVariableName()}"
        }
        res+="}}"
        return res
    }

    fun clearLists(){
        mapping.clear()
        selection.clear()
        select_body.clear()
    }

    fun getRelevantGene(action: RestCallAction, gene: Gene): Gene{
        val selectedField = selection[Pair(action.id, gene)]

        return (mapping[Pair(action.id, gene)] as ObjectGene).fields
                .filter { f -> f.name === selectedField?.second }
                .first()
    }
    fun isEmpty(): Boolean{
        return mapping.isEmpty()
    }
}
