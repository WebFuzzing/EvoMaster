package org.evomaster.core.problem.rest.util

import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.core.problem.rest.resource.CallsTemplate
import org.evomaster.core.search.service.Randomness


class RestResourceTemplateHandler{

    companion object {


        private val arrayHttpVerbs : Array<HttpVerb> = arrayOf(HttpVerb.POST, HttpVerb.GET, HttpVerb.PUT, HttpVerb.PATCH,HttpVerb.DELETE, HttpVerb.OPTIONS, HttpVerb.HEAD)
        const val SeparatorTemplate = "-"


        fun getIndexOfHttpVerb (verb: HttpVerb) : Int = arrayHttpVerbs.indexOf(verb)

        fun getSizeOfHandledVerb () : Int = arrayHttpVerbs.size

        fun parseTemplate (temp : String): Array<HttpVerb>{
            return temp.split(SeparatorTemplate).map { HttpVerb.valueOf(it) }.toTypedArray()
        }

        private fun combination(space : Array<HttpVerb>, len : Int = 5, excluding: Array<HttpVerb>? = null) : Array<String>{
            var cspace = if(excluding!= null) space.filter { !excluding!!.contains(it) }.toList() else space.toList()
            var nums = List(len){ i -> cspace.size - i}
            var result = Array<String>(nums.reduce { acc, i ->  acc * i}){""}

            for (ei in 0 until result.size){
                var t = ei
                var remove : MutableList<String> = mutableListOf()
                for(id in 0 until len-1){
                    var l = (nums.subList(id+1, len).reduce { acc, i -> acc * i })
                    var i = t/l
                    var value = cspace.filter { r -> !remove.contains(r.toString()) }[i].toString()
                    result[ei] = result[ei] + value + SeparatorTemplate
                    remove.add(value)
                    t -= l * i
                }
                result[ei] = result[ei] + cspace.filter { r -> !remove.contains(r.toString()) }[t].toString()
            }

            return result
        }
        fun initSampleSpaceOnlyPOST(_space : Array<Boolean>, maps : MutableMap<String , CallsTemplate>) {
            val space = arrayHttpVerbs.filterIndexed{index, _ ->  _space[index]}
            (if(_space.first() && !_space.last()) space.subList(1, space.size) else space).forEach {v->
                maps.getOrPut(v.toString()){ CallsTemplate(v.toString(), v != HttpVerb.POST, 1) }
            }

            if(_space.first() || _space.last()){
                val chosen = space.filter { v-> v!=HttpVerb.POST && v!=HttpVerb.HEAD && v!=HttpVerb.OPTIONS }.toTypedArray()
                chosen.forEach {
                    maps.getOrPut(HttpVerb.POST.toString()+ SeparatorTemplate+it.toString()){
                       CallsTemplate(HttpVerb.POST.toString()+ SeparatorTemplate+it.toString().toString(), false, 2)
                    }
                }
            }
        }

        fun initSampleSpace(_space : Array<Boolean>, maps : MutableMap<String , Int>) {
            val space = arrayHttpVerbs.filterIndexed{index, _ ->  _space[index]}
            (if(_space.first() && !_space.last())space.subList(1, space.size) else space).forEach {v->
                maps.getOrPut(v.toString()){0}
            }
            val chosen = space.filter { v-> v != HttpVerb.DELETE && v!=HttpVerb.POST }.toTypedArray()

            if(chosen.isEmpty()){
                if(space.size > 1)
                    maps.getOrPut(space.joinToString(SeparatorTemplate)){0}
            }else if(chosen.size == 1){
                if(_space.first()){
                    maps.getOrPut(HttpVerb.POST.toString()+ SeparatorTemplate+chosen[0]){0}
                }
                if(_space[4]){
                    maps.getOrPut(chosen[0].toString()+ SeparatorTemplate+HttpVerb.DELETE.toString()){0}
                }
                if(_space.first() && _space[4]){
                    maps.getOrPut(HttpVerb.POST.toString()+ SeparatorTemplate+chosen[0].toString()+ SeparatorTemplate+HttpVerb.DELETE.toString()){0}
                }
            }else{
                (2 until chosen.size +1).forEach{len->
                    combination(chosen,len).forEach {tmp->
                        maps.getOrPut(tmp){0}
                        if(_space.first()){
                            maps.getOrPut(HttpVerb.POST.toString()+ SeparatorTemplate+tmp){0}
                        }
                        if(_space[4]){
                            maps.getOrPut(tmp+ SeparatorTemplate+HttpVerb.DELETE.toString()){0}
                        }
                        if(_space.first() && _space[4]){
                            maps.getOrPut(HttpVerb.POST.toString()+ SeparatorTemplate+tmp+ SeparatorTemplate+HttpVerb.DELETE.toString()){0}
                        }
                    }
                }
            }
        }

        fun sample(_space : Array<Boolean>, randomness: Randomness, slen : Int = 0) : String{
            val space = arrayHttpVerbs.filterIndexed{index, _ ->  _space[index]}
            val len = if(slen != 0) slen else randomness.nextInt(1,space.size)
            if(len == 1){
                return randomness.choose(if(_space.first() && !_space.last())space.subList(1, space.size) else space).toString()
            }else{
                if(len == space.size && len - (if(_space.first()) 1 else 0) - (if(_space[4]) 1 else 0) == 1)
                    return space.map { it.toString()}.joinToString(SeparatorTemplate)
                var remove : MutableList<String> = mutableListOf()
                var result = Array(len){i->
                    if(i > 0) remove.add(HttpVerb.POST.toString())
                    if(i < len -1) remove.add(HttpVerb.DELETE.toString())
                    if(i == len -1) remove.remove(HttpVerb.DELETE.toString())
                    var chosen = randomness.choose(space.filter { v-> !remove.contains(v.toString())}).toString()
                    remove.add(chosen)
                    chosen
                }
               // assert(result.map { if(it.isNotBlank()) 1 else 0}.sum() == result.size)
                return result.joinToString(SeparatorTemplate)
            }


        }

        fun sampleAll(_space : Array<Boolean>, randomness: Randomness, slen : Int = 0) : String{
            var space = arrayHttpVerbs.filterIndexed{index, _ ->  if(index == 0) _space.last() else _space[index]}

            var len = if(slen != 0) slen else randomness.nextInt(1,space.size)
            var nums = List(len){ i -> space.size - i}

            var remove : MutableList<String> = mutableListOf()
            var result = ""
            var t = randomness.nextInt(nums.reduce { acc, i -> acc * i })
            for(id in 0 until len-1){
                var l = (nums.subList(id+1, len).reduce { acc, i -> acc * i })
                var i = t/l
                var value = space.filter { r -> !remove.contains(r.toString()) }[i].toString()
                result = result + value + SeparatorTemplate
                remove.add(value)
                t -= l * i
            }
            return result + space.filter { r -> !remove.contains(r.toString()) }[t].toString()
        }
    }
}
