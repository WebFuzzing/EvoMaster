package org.evomaster.experiments.objects

import org.evomaster.core.search.gene.*
import org.evomaster.core.problem.rest.RestAction

class UsedObj(//val mapping:MutableMap<Pair<ObjRestCallAction, Gene> , Gene> = mutableMapOf(),
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
    /*
    fun copy(): UsedObj{
        val mapcopy: MutableMap<Pair<ObjRestCallAction, Gene> , Gene> = mutableMapOf()
        val selcopy: MutableMap<Pair<ObjRestCallAction, Gene>, Pair<String, String>> = mutableMapOf()
        val bodycopy: MutableMap<ObjRestCallAction, Gene> = mutableMapOf()
        mapping.forEach { k, v ->
            mapcopy[k] = v
        }
        selection.forEach{ k, v ->
            //val copykey = Pair(k.first, k.second)
            //val copyval = Pair(v.first, v.second)
            selcopy[k] = v
        }
        select_body.forEach { k, v ->
            bodycopy[k] = v
        }

        return UsedObj(mapcopy, selcopy, bodycopy)
    }
    */
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

    fun pruneObjects(actions: MutableList<RestAction>){
        //Fix this if it is ever used again. Otherwise, scrap it.
        mapping.forEach { if(!actions.any{action -> (action as ObjRestCallAction).resolvedPath().contains(it.key.first)}) {mapping.remove(it.key)}   }
        selection.forEach {  if(!actions.any{action -> (action as ObjRestCallAction).resolvedPath().contains(it.key.first)})  {selection.remove(it.key)} }
        select_body.forEach {  if(!actions.any{action -> (action as ObjRestCallAction).resolvedPath().contains(it.key)})  {select_body.remove(it.key)} }
    }

    /*

    Leftover from mapping:MutableMap<Pair<ObjRestCallAction, Gene> , Gene>

    fun assign(key:Pair<ObjRestCallAction, Gene>, value:Gene, selectedField:Pair<String, String>){
        mapping[key] = value
        selection[key] = selectedField
    }

    fun pruneObjects(actions: MutableList<RestAction>){
        mapping.forEach { if(!actions.contains(it.key.first)) {mapping.remove(it.key)} }
        selection.forEach { if(!actions.contains(it.key.first)) {selection.remove(it.key)} }
        select_body.forEach { if(!actions.contains(it.key)) {select_body.remove(it.key)} }
    }
    fun assign(key:Pair<String, String>, value:Gene, selectedField:Pair<String, String>){
        mapping[key] = value
        selection[key] = selectedField
    }

    */

    fun assign(key:Pair<ObjRestCallAction, Gene>, value:Gene, selectedField:Pair<String, String>){
        mapping[Pair(key.first.getName(), key.second.getVariableName())] = value
        selection[Pair(key.first.getName(), key.second.getVariableName())] = selectedField
    }

    fun selectbody(action:ObjRestCallAction, obj:Gene){
        select_body[action.getName()] = obj
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
        val selectedField = selection[Pair(action.getName(), gene.getVariableName())]
        //TODO: this does not work this way. Perhaps compare by resolvedPath
        // action (when mutated) is done by copy. So action and the keys stored here will DEFINITELY not be the same object
        // can I key this by action.resolvePath()?

        val returningGene = (mapping[Pair(action.getName(), gene.getVariableName())] as ObjectGene).fields
                .filter { f -> f.name === selectedField?.second }
                .first()
        return returningGene
    }

}
