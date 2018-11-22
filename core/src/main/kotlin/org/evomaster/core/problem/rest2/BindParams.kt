package org.evomaster.core.problem.rest.serviceII

import org.evomaster.core.problem.rest.RestPath
import org.evomaster.core.problem.rest.param.*
import org.evomaster.core.search.gene.*
import java.lang.IllegalArgumentException

class BindParams {

    companion object {
        private val separator = "@"

        fun bindCreatePost(target : Param, params: List<Param>){
            if(!(target is BodyParam) || params.size != 1 || !(params[0] is BodyParam))
                throw IllegalArgumentException("select wrong bind enter point")
            (target as BodyParam).gene.copyValueFrom(params[0].gene)
        }

        fun bindParam(target : Param, targetPath: RestPath, sourcePath: RestPath, params: List<Param>){
            when(target){
                is BodyParam -> BindParams.bindBodyParam(target, targetPath,sourcePath, params)
                is PathParam -> BindParams.bindPathParm(target, targetPath,sourcePath, params)
                is QueryParam -> BindParams.bindQueryParm(target, targetPath,sourcePath, params)
                is FormParam -> BindParams.bindFormParam(target, targetPath,sourcePath, params)
                is HeaderParam -> BindParams.bindHeaderParam(target, targetPath,sourcePath, params)
            }
        }
        fun bindPathParm(p : PathParam, targetPath: RestPath, sourcePath: RestPath, params: List<Param>){
            val k = params.find { pa -> pa is PathParam && pa.name == p.name }
            if(k != null) p.gene.copyValueFrom(k!!.gene)
            else{
                if(numOfBodyParam(params) == params.size){
//                    val targetGene = (p.gene as DisruptiveGene<*>).gene
                    bindBodyAndOther(params.first{ pa -> pa is BodyParam }!! as BodyParam, sourcePath, p, targetPath,false)
                }else
                    failedToBind(p.name)
            }
        }

        //bind GET with 1) DELETE, PUT, PATCH, 2) POST
        fun bindQueryParm(p : QueryParam, targetPath: RestPath, sourcePath: RestPath, params: List<Param>){
            if(numOfBodyParam(params) == params.size){
                //bindBodyParamWithGene(params.first{ pa -> pa is BodyParam }!! as BodyParam, p.name, p.gene, false)
                bindBodyAndOther(params.first{ pa -> pa is BodyParam }!! as BodyParam, sourcePath, p, targetPath,false)
            }else{
                val sg = params.filter { pa -> !(pa is BodyParam) }.find { pa -> pa.name == p.name && p.gene::class.java.simpleName == pa.gene::class.java.simpleName}
                if(sg != null)
                    p.gene.copyValueFrom(sg.gene)
                else
                    failedToBind(p.name)
            }
        }

        fun bindBodyParam(bp : BodyParam, targetPath: RestPath, sourcePath: RestPath, params: List<Param>){
            if(numOfBodyParam(params) != params.size ){
                params.filter { p -> !(p is BodyParam) }
                        .forEach {ip->
                            bindBodyAndOther(bp, targetPath, ip, sourcePath, true)
                        }
            }else if(params.isNotEmpty()){
                if((bp.gene as ObjectGene).fields.map { g -> g.name }.containsAll((params[0].gene as ObjectGene).fields.map { g -> g.name })){
                    bp.gene.copyValueFrom(params[0].gene)
                }

                //throw java.lang.IllegalArgumentException("it does not allow to bind POST $targetPath with POST $sourcePath")
            }



        }

        fun bindHeaderParam(p : HeaderParam, targetPath: RestPath, sourcePath: RestPath, params: List<Param>){
            //TODO

        }

        fun bindFormParam(p : FormParam, targetPath: RestPath, sourcePath: RestPath, params: List<Param>){
            //TODO
        }

        fun bindBodyAndOther(body : BodyParam, bodyPath:RestPath, other : Param, otherPath : RestPath, b2g: Boolean){
            val pathMap = geneNameMaps(listOf(other), otherPath.getStaticTokens().reversed())
            val bodyMap = geneNameMaps(listOf(body), bodyPath.getStaticTokens().reversed())
            pathMap.forEach { pathkey, pathGene ->
                if(bodyMap.get(pathkey) != null){
                    copyGene(bodyMap.getValue(pathkey), pathGene, b2g)
                }else{
                    val matched = bodyMap.keys.filter { s -> scoreOfMatch(pathkey, s) == 0 }
                    if(matched.isNotEmpty()){
                        val first = matched.first()
                        copyGene(bodyMap.getValue(first), pathGene, b2g)
                    }else{
                        failedToBind(pathkey)
                    }
                }
            }
        }

        fun failedToBind(msg : String = ""){
            //println("$msg failed to bind anything")
        }

        //FIXME check whether params can contain two body params?
        fun numOfBodyParam(params: List<Param>) : Int{
            params.filter { it is BodyParam }?.let {
                return it.size
            }
            return 0
        }


        fun copyGene(b : Gene, g : Gene, b2g :Boolean){
            if(b::class.java.simpleName == g::class.java.simpleName){
                if (b2g) b.copyValueFrom(g)
                else g.copyValueFrom(b)
            }else{
                val first = if(b2g) b else g
                val second = if(b2g) g else b
                //FIXME covert StringGene to other type if required, and currently only support StringGene to LongGene for handling "id" attribute in some cases
                if(first is StringGene)
                    first.value = second.getValueAsRawString()
                else{
                    if (first is LongGene){
                        val sv = second.getValueAsRawString().toLongOrNull()
                        if(sv != null)
                            first.value = sv
                        else if(second is StringGene){
                            second.value = first.getValueAsRawString()
                        }
                    }
                }
            }
        }

        fun scoreOfMatch(target : String, source : String) : Int{
            //FIXME if "d_" is a real attribute of some classes
            val targets = target.split(separator).filter { it != "d_" }.toMutableList()
            val sources = source.split(separator).filter { it != "d_" }.toMutableList()
            if(targets.first() != sources.first()) return -1
            if(targets.toHashSet().size == sources.toHashSet().size){
                if(targets.containsAll(sources)) return 0
            }
            return targets.plus(sources).filter { targets.contains(it).xor(sources.contains(it)) }.size
        }

        fun geneNameMaps(parameters: List<Param>, tokensInPath: List<String>?) : MutableMap<String, Gene>{
            val maps = mutableMapOf<String, Gene>()
            val pred = {gene : Gene -> (gene is DateTimeGene)}
            parameters.forEach { p->
                p.gene.flatViewWithTypeFilter(pred).filter {
                    !(it is ObjectGene ||
                            it is DisruptiveGene<*> ||
                            it is OptionalGene ||
                            it is ArrayGene<*> ||
                            it is MapGene<*>) }
                        .forEach { g->
                            val names = getGeneNamesInPath(parameters, g)
                            if(names != null)
                                maps.put(genGeneNameInPath(names, tokensInPath), g)
                        }
            }

            return maps
        }

        fun genGeneNameInPath(names : MutableList<String>, tokensInPath : List<String>?) : String{
            tokensInPath?.let {
                return names.plus(tokensInPath).joinToString(separator)
            }
            return names.joinToString(separator)
        }

        fun getGeneNamesInPath(parameters: List<Param>, gene : Gene) : MutableList<String>? {
            parameters.forEach {  p->
                val names = mutableListOf<String>()
                if(handle(p.gene, gene, names)){
                    return names
                }
            }

            return null
        }

        fun handle(comGene : Gene, gene: Gene, names : MutableList<String>) : Boolean{
            when(comGene){
                is ObjectGene -> return handle(comGene, gene, names)
                is DisruptiveGene<*> -> return handle(comGene, gene, names)
                is OptionalGene -> return handle(comGene, gene, names)
                is ArrayGene<*> -> return handle(comGene, gene, names)
                is MapGene<*> -> return handle(comGene, gene, names)
                else -> if (comGene == gene) {
                    names.add(comGene.name)
                    return true
                }else return false
            }
        }

        fun handle(comGene : ObjectGene, gene: Gene, names: MutableList<String>) : Boolean{
            comGene.fields.forEach {
                if(handle(it, gene, names)){
                    names.add(it.name)
                    return true
                }

            }
            return false
        }

        fun handle(comGene : DisruptiveGene<*>, gene: Gene, names: MutableList<String>) : Boolean{
            if(handle(comGene.gene, gene, names)){
                names.add(comGene.name)
                return true
            }
            return false
        }

        fun handle(comGene : OptionalGene, gene: Gene, names: MutableList<String>) : Boolean{
            if(handle(comGene.gene, gene, names)){
                names.add(comGene.name)
                return true
            }
            return false
        }

        fun handle(comGene : ArrayGene<*>, gene: Gene, names: MutableList<String>): Boolean{
            comGene.elements.forEach {
                if(handle(it, gene, names)){
                    return true
                }
            }
            return false
        }

        fun handle(comGene : MapGene<*>, gene: Gene, names: MutableList<String>) : Boolean{
            comGene.elements.forEach {
                if(handle(it, gene, names)){
                    names.add(it.name)
                    return true
                }
            }
            return false
        }
    }
}