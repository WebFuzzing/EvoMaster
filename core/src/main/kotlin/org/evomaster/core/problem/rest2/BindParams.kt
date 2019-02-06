package org.evomaster.core.problem.rest.serviceII

import org.evomaster.core.problem.rest.RestPath
import org.evomaster.core.problem.rest.param.*
import org.evomaster.core.search.gene.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.IllegalArgumentException
import kotlin.math.min

class BindParams {

    companion object {

        //FIXME
        var disableLog = true
        private const val separator = "@"

        private val log: Logger = LoggerFactory.getLogger(BindParams::class.java)

        fun bindCreatePost(target : Param, params: List<Param>){
            if(!(target is BodyParam) || params.size != 1 || !(params[0] is BodyParam))
                throw IllegalArgumentException("select wrong bind enter point")
            (target as BodyParam).gene.copyValueFrom(params[0].gene)
        }

        fun bindParam(target : Param, targetPath: RestPath, sourcePath: RestPath, params: List<Param>, inner : Boolean = false){
            when(target){
                is BodyParam -> BindParams.bindBodyParam(target, targetPath,sourcePath, params, inner)
                is PathParam -> BindParams.bindPathParm(target, targetPath,sourcePath, params, inner)
                is QueryParam -> BindParams.bindQueryParm(target, targetPath,sourcePath, params, inner)
                is FormParam -> BindParams.bindFormParam(target, targetPath,sourcePath, params)
                is HeaderParam -> BindParams.bindHeaderParam(target, targetPath,sourcePath, params)
            }
        }
        private fun bindPathParm(p : PathParam, targetPath: RestPath, sourcePath: RestPath, params: List<Param>, inner : Boolean){
            val k = params.find { pa -> pa is PathParam && pa.name == p.name }
            if(k != null) p.gene.copyValueFrom(k!!.gene)
            else{
                if(numOfBodyParam(params) == params.size){
                    bindBodyAndOther(params.first{ pa -> pa is BodyParam }!! as BodyParam, sourcePath, p, targetPath,false, inner)
                }else
                    if(!disableLog) log.warn("cannot find PathParam ${p.name} in params ${params.mapNotNull { it.name }.joinToString(" ")}")
            }
        }

        private fun bindQueryParm(p : QueryParam, targetPath: RestPath, sourcePath: RestPath, params: List<Param>, inner : Boolean){
            if(params.isNotEmpty() && numOfBodyParam(params) == params.size){
                bindBodyAndOther(params.first{ pa -> pa is BodyParam }!! as BodyParam, sourcePath, p, targetPath,false, inner)
            }else{
                val sg = params.filter { pa -> !(pa is BodyParam) }.find { pa -> pa.name == p.name && p.gene::class.java.simpleName == pa.gene::class.java.simpleName}
                if(sg != null)
                    p.gene.copyValueFrom(sg.gene)
                else
                    if(!disableLog) log.warn("cannot find QueryParam ${p.name} in params ${params.map { it.name }.joinToString(" ")}")
            }
        }

        private fun bindBodyParam(bp : BodyParam, targetPath: RestPath, sourcePath: RestPath, params: List<Param>, inner : Boolean){
            if(numOfBodyParam(params) != params.size ){
                params.filter { p -> !(p is BodyParam) }
                        .forEach {ip->
                            bindBodyAndOther(bp, targetPath, ip, sourcePath, true, inner)
                        }
            }else if(params.isNotEmpty()){
                if((bp.gene as ObjectGene).fields.map { g -> g.name }.containsAll((params[0].gene as ObjectGene).fields.map { g -> g.name })){
                    bp.gene.copyValueFrom(params[0].gene)
                }

                //throw java.lang.IllegalArgumentException("it does not allow to bind POST $targetPath with POST $sourcePath")
            }
        }

        private fun bindHeaderParam(p : HeaderParam, targetPath: RestPath, sourcePath: RestPath, params: List<Param>){
            params.find { it is HeaderParam && p.name == it.name}?.apply {
                p.gene.copyValueFrom(this.gene)
            }
        }

        private fun bindFormParam(p : FormParam, targetPath: RestPath, sourcePath: RestPath, params: List<Param>){
            params.find { it is FormParam && p.name == it.name}?.apply {
                p.gene.copyValueFrom(this.gene)
            }
        }

        private fun bindBodyAndOther(body : BodyParam, bodyPath:RestPath, other : Param, otherPath : RestPath, b2g: Boolean, inner : Boolean){
            val pathMap = geneNameMaps(listOf(other), otherPath.getStaticTokens().reversed())
            val bodyMap = geneNameMaps(listOf(body), bodyPath.getStaticTokens().reversed())
            pathMap.forEach { pathkey, pathGene ->
                if(bodyMap.get(pathkey) != null){
                    copyGene(bodyMap.getValue(pathkey), pathGene, b2g)
                }else{
                    val matched = bodyMap.keys.filter { s -> scoreOfMatch(pathkey, s, inner) == 0 }
                    if(matched.isNotEmpty()){
                        val first = matched.first()
                        copyGene(bodyMap.getValue(first), pathGene, b2g)
                    }else{
                        if(inner){
                            if(!disableLog) log.info("cannot find ${pathkey} in its bodyParam ${bodyMap.keys.joinToString(" ")}")
                        }else
                            if(!disableLog) log.warn("cannot find ${pathkey} in bodyParam ${bodyMap.keys.joinToString(" ")}")
                    }
                }
            }
        }

        //FIXME check whether params can contain two body params?
        fun numOfBodyParam(params: List<Param>) : Int{
            params.filter { it is BodyParam }?.let {
                return it.size
            }
            return 0
        }


        private fun copyGene(b : Gene, g : Gene, b2g :Boolean){
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

        private fun compareParamNames(a : String, b : String) : Boolean{
            return a.toLowerCase().contains(b.toLowerCase()) || b.toLowerCase().contains(a.toLowerCase())
        }

        private fun scoreOfMatch(target : String, source : String, inner : Boolean) : Int{
            //FIXME if "d_" is a real attribute of some classes
            val targets = target.split(separator).filter { it != "d_"  }.toMutableList()
            val sources = source.split(separator).filter { it != "d_"  }.toMutableList()
            //if(inner){
            val removed = sources.map { it }.toMutableList()
            removed.remove("register")
            if(sources.toHashSet().map { s -> if(target.toLowerCase().contains(s.toLowerCase()))1 else 0}.sum() == sources.toHashSet().size)
                return 0
            if(removed.toHashSet().map { s -> if(target.toLowerCase().contains(s.toLowerCase()))1 else 0}.sum() == removed.toHashSet().size)
                return 0

            //}
            if(targets.toHashSet().size == sources.toHashSet().size){
                if(targets.containsAll(sources)) return 0
            }
            if(sources.contains("body")){
                val sources_rbody = sources.filter { it != "body"  }.toMutableList()
                if(sources_rbody.toHashSet().map { s -> if(target.toLowerCase().contains(s.toLowerCase()))1 else 0}.sum() == sources_rbody.toHashSet().size)
                    return 0
                val removed = sources_rbody.map { it }.toMutableList()
                removed.remove("register")
                if(removed.toHashSet().map { s -> if(target.toLowerCase().contains(s.toLowerCase()))1 else 0}.sum() == removed.toHashSet().size)
                    return 0
//                removed.map { c -> if(c.contains("_")) c.split("_").first() else c }
//                if(removed.toHashSet().map { s -> if(target.toLowerCase().contains(s.toLowerCase()))1 else 0}.sum() == removed.toHashSet().size)
//                    return 0
            }

            //val repeat = targets.plus(sources).filter { targets.contains(it).xor(sources.contains(it)) }.size
            if(targets.first() != sources.first())
                return -1
            else
                return targets.plus(sources).filter { targets.contains(it).xor(sources.contains(it)) }.size

        }

        private fun geneNameMaps(parameters: List<Param>, tokensInPath: List<String>?) : MutableMap<String, Gene>{
            val maps = mutableMapOf<String, Gene>()
            val pred = {gene : Gene -> (gene is DateTimeGene)}
            parameters.forEach { p->
                p.gene.flatView(pred).filter {
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

        private fun genGeneNameInPath(names : MutableList<String>, tokensInPath : List<String>?) : String{
            tokensInPath?.let {
                return names.plus(tokensInPath).joinToString(separator)
            }
            return names.joinToString(separator)
        }

        private fun getGeneNamesInPath(parameters: List<Param>, gene : Gene) : MutableList<String>? {
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

        fun getParamId(param : Param, path : RestPath) : String{
            return listOf(param.name).plus(path.getStaticTokens().reversed()).joinToString(separator)
        }
    }
}