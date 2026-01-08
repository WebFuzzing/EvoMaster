package org.evomaster.core.problem.util

import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.resource.CallsTemplate
import org.evomaster.core.problem.rest.resource.RestResourceCalls
import org.evomaster.core.search.action.ActionFilter
import org.evomaster.core.search.service.Randomness


/**
 * utility to handle template of [RestResourceCall]
 */
class RestResourceTemplateHandler{

    companion object {


        private val arrayHttpVerbs : Array<HttpVerb> =
                arrayOf(
                    HttpVerb.POST,
                        HttpVerb.GET,
                        HttpVerb.PUT,
                        HttpVerb.PATCH,
                        HttpVerb.DELETE,
                        HttpVerb.OPTIONS,
                        HttpVerb.HEAD,
                        HttpVerb.TRACE)
        private const val SeparatorTemplate = "-"


        fun getIndexOfHttpVerb (verb: HttpVerb) : Int = arrayHttpVerbs.indexOf(verb)

        fun getSizeOfHandledVerb () : Int = arrayHttpVerbs.size

        fun isNotSingleAction(template : String) : Boolean = template.contains(SeparatorTemplate) && validateTemplate(template)

        private fun validateTemplate(template: String) : Boolean = template.split(SeparatorTemplate).all { arrayHttpVerbs.any { e->e.toString() == it } }

        fun parseTemplate (temp : String): Array<HttpVerb>{
            return temp.split(SeparatorTemplate).map { HttpVerb.valueOf(it) }.toTypedArray()
        }

        fun getStringTemplateByActions(actions : List<RestCallAction>) : String{
            return formatTemplate(actions.map { it.verb }.toTypedArray())
        }

        fun getStringTemplateByCalls(calls: RestResourceCalls) : String{
            val verbs = mutableListOf((calls.seeActions(ActionFilter.NO_SQL).last() as RestCallAction).verb)
            if (calls.seeActions(ActionFilter.ONLY_SQL).isNotEmpty() || calls.seeActions(ActionFilter.NO_SQL).size > 1)
                verbs.add(0, HttpVerb.POST)
            return  formatTemplate(verbs.toTypedArray())
        }

        private fun combination(space : Array<HttpVerb>, len : Int = 5, excluding: Array<HttpVerb>? = null) : Array<String>{
            val cspace = if(excluding!= null) space.filter { !excluding.contains(it) }.toList() else space.toList()
            val nums = List(len){ i -> cspace.size - i}
            val result = Array(nums.reduce { acc, i ->  acc * i}){""}

            for (ei in 0 until result.size){
                var t = ei
                val remove : MutableList<String> = mutableListOf()
                for(id in 0 until len-1){
                    val l = (nums.subList(id+1, len).reduce { acc, i -> acc * i })
                    val i = t/l
                    val value = cspace.filter { r -> !remove.contains(r.toString()) }[i].toString()
                    result[ei] = result[ei] + value + SeparatorTemplate
                    remove.add(value)
                    t -= l * i
                }
                result[ei] = result[ei] + cspace.filter { r -> !remove.contains(r.toString()) }[t].toString()
            }

            return result
        }
        fun initSampleSpaceOnlyPOST(_space : Array<Boolean>, maps : MutableMap<String , CallsTemplate>) {
            val space = arrayHttpVerbs.filterIndexed{ index, _ ->  _space[index]}
            (if(_space.first() && !_space.last()) space.subList(1, space.size) else space).forEach {v->
                maps.getOrPut(v.toString()){ CallsTemplate(v.toString(), v != HttpVerb.POST, 1) }
            }

            val createActionVerb = if(_space.first() || _space.last()){
                HttpVerb.POST
            }else if(_space[getIndexOfHttpVerb(HttpVerb.PUT)]){
                HttpVerb.PUT
            }else null

            if (createActionVerb != null){
                val chosen = space.filter { v-> v!= HttpVerb.HEAD && v!= HttpVerb.OPTIONS }.toTypedArray()
                chosen.forEach {
                    val key = formatTemplate(arrayOf(createActionVerb, it))
                    maps.getOrPut(key){
                        CallsTemplate(key, false, 2)
                    }
                }
            }

        }

        private fun formatTemplate(verbs : Array<HttpVerb>) : String = verbs.joinToString(SeparatorTemplate)

        private fun formatTemplate(stringVerbs : Array<String>) : String = stringVerbs.joinToString(SeparatorTemplate)


        fun sample(_space : Array<Boolean>, randomness: Randomness, slen : Int = 0) : String{
            val space = arrayHttpVerbs.filterIndexed{ index, _ ->  _space[index]}
            val len = if(slen != 0) slen else randomness.nextInt(1,space.size)
            if(len == 1){
                return randomness.choose(if(_space.first() && !_space.last())space.subList(1, space.size) else space).toString()
            }else{
                if(len == space.size && len - (if(_space.first()) 1 else 0) - (if(_space[4]) 1 else 0) == 1)
                    return formatTemplate(space.toTypedArray())
                val remove : MutableList<String> = mutableListOf()
                val result = Array(len){i->
                    if(i > 0) remove.add(HttpVerb.POST.toString())
                    if(i < len -1) remove.add(HttpVerb.DELETE.toString())
                    if(i == len -1) remove.remove(HttpVerb.DELETE.toString())
                    val chosen = randomness.choose(space.filter { v-> !remove.contains(v.toString())}).toString()
                    remove.add(chosen)
                    chosen
                }
               // assert(result.map { if(it.isNotBlank()) 1 else 0}.sum() == result.size)
                return result.joinToString(SeparatorTemplate)
            }


        }

    }
}
