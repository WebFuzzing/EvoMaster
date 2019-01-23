package org.evomaster.experiments.objects

import org.evomaster.core.search.gene.DisruptiveGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.OptionalGene

class UsedObj(var mapping:MutableMap<Pair<ObjRestCallAction, Gene> , ObjectGene> = mutableMapOf(),
              var selection:MutableMap<Pair<ObjRestCallAction, Gene>, Pair<String, String>> = mutableMapOf(),
              var select_body:MutableMap<ObjRestCallAction, ObjectGene> = mutableMapOf()){

    fun copy(): UsedObj{
        val mapcopy: MutableMap<Pair<ObjRestCallAction, Gene> , ObjectGene> = mutableMapOf()
        val selcopy: MutableMap<Pair<ObjRestCallAction, Gene>, Pair<String, String>> = mutableMapOf()
        val bodycopy: MutableMap<ObjRestCallAction, ObjectGene> = mutableMapOf()
        mapping.forEach { k, v ->
            mapcopy[Pair(k.first, k.second)] = v
        }
        selection.forEach{ k, v ->
            val copykey = Pair(k.first, k.second)
            val copyval = Pair(v.first, v.second)
            selcopy[copykey] = copyval
        }
        select_body.forEach { k, v ->
            bodycopy[k] = v
        }

        return UsedObj(mapcopy, selcopy, bodycopy)
    }

    fun usedObjects(): List<Gene>{
        //return all objects for mutation and randomization purposes
        return mapping.values.flatMap{ it -> it.flatView()}
    }

    fun coherenceCheck(){
        mapping.forEach { key, value ->
            val sel = selection[key] as Pair<String, String>
            val v = value.fields.filter { g -> (g as OptionalGene).name === sel.second }?.single() as OptionalGene
            when (key.second::class) {
                DisruptiveGene::class -> (key.second as DisruptiveGene<*>).gene.copyValueFrom(v.gene)
                OptionalGene::class ->  (key.second as OptionalGene).gene.copyValueFrom(v.gene)
            }
        }
    }

    fun assign(key:Pair<ObjRestCallAction, Gene>, value:ObjectGene, selectedField:Pair<String, String>){
        mapping[key] = value
        selection[key] = selectedField
    }

    fun selectbody(action:ObjRestCallAction, obj:ObjectGene){
        select_body[action] = obj
    }


    fun displayInline() : String{
        //display inline (mostly for debugging)
        var res = "{{"
        mapping.forEach { k, v ->
            res += "[${k.first.toString()}, ${k.second.getVariableName()}]=${v.getVariableName()}"
        }
        res+="}}"
        return res
    }

    fun clearLists(){
        mapping.clear()
        selection.clear()
        select_body.clear()
    }

}
