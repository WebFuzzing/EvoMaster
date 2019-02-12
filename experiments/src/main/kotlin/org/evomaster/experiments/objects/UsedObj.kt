package org.evomaster.experiments.objects

import org.evomaster.core.search.gene.*

class UsedObj(
              val mapping:MutableMap<Pair<String, String> , Gene> = mutableMapOf(),
              val selection:MutableMap<Pair<String, String>, Pair<String, String>> = mutableMapOf(),
              val select_body:MutableMap<String, Gene> = mutableMapOf()){

    fun copy(): UsedObj{
        val mapcopy: MutableMap<Pair<String, String> , Gene> = mutableMapOf()
        val selcopy: MutableMap<Pair<String, String>, Pair<String, String>> = mutableMapOf()
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
        mapping.forEach { key, value ->
            when (value::class){
                ObjectGene::class -> {
                    val sel = selection[key] as Pair<String, String>
                    var v: OptionalGene
                    if (sel.second === ""){
                        v = OptionalGene("temp", (value as ObjectGene) )
                    }
                    else {
                        v = (value as ObjectGene).fields.filter { g -> (g as OptionalGene).name === sel.second }?.single() as OptionalGene
                    }
                    when (key.second::class) {
                        DisruptiveGene::class -> (key.second as DisruptiveGene<*>).gene.copyValueFrom(v.gene)
                        OptionalGene::class ->  (key.second as OptionalGene).gene.copyValueFrom(v.gene)
                        ObjectGene::class -> (key.second as ObjectGene).copyValueFrom(v.gene)
                    }
                }
            }

        }
    }

    fun assign(key:Pair<ObjRestCallAction, Gene>, value:Gene, selectedField:Pair<String, String>){
        mapping[Pair(key.first.id, key.second.getVariableName())] = value
        selection[Pair(key.first.id, key.second.getVariableName())] = selectedField
    }

    fun selectbody(action:ObjRestCallAction, obj:Gene){
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

    fun getRelevantGene(action: ObjRestCallAction, gene: Gene): Gene{
        val selectedField = selection[Pair(action.id, gene.getVariableName())]

        return (mapping[Pair(action.id, gene.getVariableName())] as ObjectGene).fields
                .filter { f -> f.name === selectedField?.second }
                .first()
    }

}
