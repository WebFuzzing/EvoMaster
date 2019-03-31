package org.evomaster.core.problem.rest.serviceII

import org.evomaster.core.database.DbAction
import org.evomaster.core.problem.rest.RestPath
import org.evomaster.core.problem.rest.param.*
import org.evomaster.core.problem.rest2.resources.token.parser.ParserUtil
import org.evomaster.core.search.gene.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.IllegalArgumentException

class ParamHandler {

    companion object {

        //FIXME
        var disableLog = true

        private const val DISRUPTIVE_NAME = "d_"
        private const val BODYGENE_NAME = "body"
        const val separator = "@"
        var GENERAL_NAMES = mutableListOf("id", "name")

        private val log: Logger = LoggerFactory.getLogger(ParamHandler::class.java)


        /**
         * @param target bind [target] based on other params, i.e., [params]
         * @param targetPath is the path of [target]
         * @param sourcePath
         * @param params
         */
        fun bindParam(target : Param, targetPath: RestPath, sourcePath: RestPath, params: List<Param>, inner : Boolean = false){
            when(target){
                is BodyParam -> bindBodyParam(target, targetPath,sourcePath, params, inner)
                is PathParam -> bindPathParm(target, targetPath,sourcePath, params, inner)
                is QueryParam -> bindQueryParm(target, targetPath,sourcePath, params, inner)
                is FormParam -> bindFormParam(target, targetPath,sourcePath, params)
                is HeaderParam -> bindHeaderParam(target, targetPath,sourcePath, params)
            }
        }
        private fun bindPathParm(p : PathParam, targetPath: RestPath, sourcePath: RestPath, params: List<Param>, inner : Boolean){
            val k = params.find { pa -> pa is PathParam && pa.name == p.name }
            if(k != null) p.gene.copyValueFrom(k!!.gene)
            else{
                if(numOfBodyParam(params) == params.size && params.isNotEmpty()){
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
                val valueGene = getValueGene(bp.gene)
                val pValueGene = getValueGene(params[0].gene)
                if(valueGene !is ObjectGene
                        || pValueGene !is ObjectGene)
                    throw IllegalArgumentException("cannot bind")

                if((valueGene as ObjectGene).fields.map { g -> g.name }.containsAll((pValueGene as ObjectGene).fields.map { g -> g.name })){
                    valueGene.copyValueFrom(pValueGene)
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
            }else if(b2g && g is SqlPrimaryKeyGene){
                copyWithTypeAdapter(b, g)
            }else if(!b2g && b is SqlPrimaryKeyGene)
                copyWithTypeAdapter(g, b)
            else{
                //TODO
                val result = if(b2g) copyWithTypeAdapter(b, g)
                            else copyWithTypeAdapter(g, b)
                if(!result){
                    if(!disableLog) log.info("$g fails to copy value from gene $g")
                }
            }
        }

        /**
         * bind [b] based on [g].
         * [b] can be one of types : DoubleGene, FloatGene, IntegerGene, LongGene, StringGene
         * [g] can be all types of [b] plus ImmutableDataHolderGene
         */
        private fun copyWithTypeAdapter(b : Gene, g : Gene) : Boolean{
            return when(b){
                is DoubleGene -> covertToDouble(b,g)
                is FloatGene -> covertToFloat(b,g)
                is IntegerGene -> covertToInteger(b,g)
                is LongGene -> covertToLong(b,g)
                is StringGene -> covertToString(b,g)
                else -> false
            }
        }

        private fun covertToDouble(b: DoubleGene, g : Gene) : Boolean{
            when(g){
                is DoubleGene -> b.value = g.value
                is FloatGene -> b.value = g.value.toDouble()
                is IntegerGene -> b.value = g.value.toDouble()
                is LongGene -> b.value = g.value.toDouble()
                is StringGene -> {
                    val value = g.value.toDoubleOrNull() ?: return false
                    b.value = value
                }
                is ImmutableDataHolderGene -> {
                    val value = g.value.toDoubleOrNull() ?: return false
                    b.value = value
                }
                is SqlPrimaryKeyGene ->{
                    b.value = g.uniqueId.toDouble()
                }
                else -> return false
            }
            return true
        }

        private fun covertToFloat(b: FloatGene, g : Gene) : Boolean{
            when(g){
                is FloatGene -> b.value = g.value
                is DoubleGene -> b.value = g.value.toFloat()
                is IntegerGene -> b.value = g.value.toFloat()
                is LongGene -> b.value = g.value.toFloat()
                is StringGene -> {
                    val value = g.value.toFloatOrNull() ?: return false
                    b.value = value
                }
                is ImmutableDataHolderGene -> {
                    val value = g.value.toFloatOrNull() ?: return false
                    b.value = value
                }
                is SqlPrimaryKeyGene ->{
                    b.value = g.uniqueId.toFloat()
                }
                else -> return false
            }
            return true
        }

        private fun covertToInteger(b: IntegerGene, g : Gene) : Boolean{
            when(g){
                is IntegerGene -> b.value = g.value
                is FloatGene -> b.value = g.value.toInt()
                is DoubleGene -> b.value = g.value.toInt()
                is LongGene -> b.value = g.value.toInt()
                is StringGene -> {
                    val value = g.value.toIntOrNull() ?: return false
                    b.value = value
                }
                is ImmutableDataHolderGene -> {
                    val value = g.value.toIntOrNull() ?: return false
                    b.value = value
                }
                is SqlPrimaryKeyGene ->{
                    b.value = g.uniqueId.toInt()
                }
                else -> return false
            }
            return true
        }

        private fun covertToLong(b: LongGene, g : Gene) : Boolean{
            when(g){
                is LongGene -> b.value = g.value
                is FloatGene -> b.value = g.value.toLong()
                is IntegerGene -> b.value = g.value.toLong()
                is DoubleGene -> b.value = g.value.toLong()
                is StringGene -> {
                    val value = g.value.toLongOrNull() ?: return false
                    b.value = value
                }
                is ImmutableDataHolderGene -> {
                    val value = g.value.toLongOrNull() ?: return false
                    b.value = value
                }
                is SqlPrimaryKeyGene ->{
                    b.value = g.uniqueId
                }
                else -> return false
            }
            return true
        }

        private fun covertToString(b: StringGene, g : Gene) : Boolean{
            when(g){
                is StringGene -> b.value = g.value
                is FloatGene -> b.value = g.value.toString()
                is IntegerGene -> b.value = g.value.toString()
                is LongGene -> b.value = g.value.toString()
                is DoubleGene -> b.value = g.value.toString()
                is ImmutableDataHolderGene -> b.value = g.value
                is SqlPrimaryKeyGene ->{
                    b.value = g.uniqueId.toString()
                }
                else -> return false
            }
            return true
        }

        private fun compareParamNames(a : String, b : String) : Boolean{
            return a.toLowerCase().contains(b.toLowerCase()) || b.toLowerCase().contains(a.toLowerCase())
        }

        private fun scoreOfMatch(target : String, source : String, inner : Boolean) : Int{
            val targets = target.split(separator).filter { it != DISRUPTIVE_NAME  }.toMutableList()
            val sources = source.split(separator).filter { it != DISRUPTIVE_NAME  }.toMutableList()
            if(inner){
                if(sources.toHashSet().map { s -> if(target.toLowerCase().contains(s.toLowerCase()))1 else 0}.sum() == sources.toHashSet().size)
                    return 0
            }
            if(targets.toHashSet().size == sources.toHashSet().size){
                if(targets.containsAll(sources)) return 0
            }
            if(sources.contains(BODYGENE_NAME)){
                val sources_rbody = sources.filter { it != BODYGENE_NAME  }.toMutableList()
                if(sources_rbody.toHashSet().map { s -> if(target.toLowerCase().contains(s.toLowerCase()))1 else 0}.sum() == sources_rbody.toHashSet().size)
                    return 0
            }
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

        fun bindParam(dbAction: DbAction, param : Param, previousToken: String, isExistingData : Boolean) : Boolean{
            var gene  : Gene? = null
            var similarity = 0.0
            dbAction.seeGenes().forEach findGene@{
                val score = compareDBGene(dbAction, it, param.name, previousToken)
                if(score>similarity){
                    similarity = score
                    gene = it
                }
                if(score == 1.0) return@findGene
            }
            if(similarity > ParserUtil.SimilarityThreshold ){
                if(gene!! is SqlPrimaryKeyGene || gene!! is SqlForeignKeyGene || gene!! is SqlAutoIncrementGene){
                    /*
                        if gene of dbaction is PK, FK or AutoIncrementGene,
                            bind gene of Param according to the gene from dbaction
                     */
                    copyGene(getValueGene(gene!!),  getValueGene(param.gene), false)
                }else{
                    /*
                        If the data is existing in db, bind gene of Param according to the gene from dbaction
                        otherwise, bind gene of action according to rest action, i.e., Gene of Param
                     */
                    copyGene(getValueGene(gene!!),  getValueGene(param.gene), !isExistingData)
                }

            }else
                return false
            return true
        }

        fun compareDBGene(dbAction: DbAction, gene: Gene, pName: String, previousToken : String) : Double{
            val list = mutableListOf<Double>()
            ParserUtil.stringSimilarityScore(gene.name, pName).apply {
                list.add(this)
                if(this == 1.0) return this
            }
            if(isGeneralName(gene.name)){
                ParserUtil.stringSimilarityScore(dbAction.table.name+gene.name, pName).apply {
                    list.add(this)
                    if(this == 1.0) return this
                }
                ParserUtil.stringSimilarityScore(gene.name, previousToken+pName).apply {
                    list.add(this)
                    if(this == 1.0) return this
                }
            }
            return list.max()!!
        }

        private fun isGeneralName(text : String) : Boolean{
            return GENERAL_NAMES.contains(text.toLowerCase())
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

        fun getValueGene(gene : Gene) : Gene{
            if(gene is OptionalGene){
                return getValueGene(gene.gene)
            }else if(gene is DisruptiveGene<*>)
                return getValueGene(gene.gene)
            else if(gene is SqlPrimaryKeyGene){
                if(gene.gene is SqlAutoIncrementGene)
                    return gene
                else return getValueGene(gene.gene)
            }
            return gene
        }



    }
}