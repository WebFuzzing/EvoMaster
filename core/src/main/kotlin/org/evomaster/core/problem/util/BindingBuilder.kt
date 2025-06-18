package org.evomaster.core.problem.util

import com.google.common.annotations.VisibleForTesting
import org.evomaster.client.java.instrumentation.shared.TaintInputName
import org.evomaster.core.Lazy
import org.evomaster.core.StaticCounter
import org.evomaster.core.sql.SqlAction
import org.evomaster.core.sql.SqlActionUtils
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.problem.api.param.Param
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestPath
import org.evomaster.core.problem.rest.param.*
import org.evomaster.core.problem.rest.resource.RestResourceNode
import org.evomaster.core.problem.util.inference.model.ParamGeneBindMap
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.gene.collection.ArrayGene
import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.collection.FixedMapGene
import org.evomaster.core.search.gene.datetime.DateGene
import org.evomaster.core.search.gene.datetime.DateTimeGene
import org.evomaster.core.search.gene.datetime.TimeGene
import org.evomaster.core.search.gene.numeric.DoubleGene
import org.evomaster.core.search.gene.numeric.FloatGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.numeric.LongGene
import org.evomaster.core.search.gene.optional.OptionalGene
import org.evomaster.core.search.gene.placeholder.CycleObjectGene
import org.evomaster.core.search.gene.placeholder.ImmutableDataHolderGene
import org.evomaster.core.search.gene.sql.SqlAutoIncrementGene
import org.evomaster.core.search.gene.sql.SqlForeignKeyGene
import org.evomaster.core.search.gene.sql.SqlPrimaryKeyGene
import org.evomaster.core.search.gene.string.Base64StringGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.service.Randomness
import org.slf4j.LoggerFactory

/**
 * gene binding builder among actions/genes for an individual
 */
object BindingBuilder {

    private val log = LoggerFactory.getLogger(BindingBuilder::class.java)

    /**
     * a probability of binding a field of object gene
     *
     * note that
     * there might be no need to bind all fields between two ObjectGenes
     * eg, POST-PUT with body param, no need to bind every field between the two body payloads
     *
     * this static variable is only changeable for testing purpose
     * see [setProbabilityOfBindingObject]
     *
     */
    private var PROBABILITY_BINDING_OBJECT = 0.5

    @VisibleForTesting
    internal fun setProbabilityOfBindingObject(probability: Double){
        PROBABILITY_BINDING_OBJECT = probability
    }

    /**
     * bind value within a rest action [restAction], e.g., PathParam with BodyParam
     * @param doBuildBindingGene specifies whether to build the binding gene
     */
    fun bindParamsInRestAction(restAction: RestCallAction, doBuildBindingGene: Boolean = false, randomness: Randomness?){
        val pairs = buildBindingPairsInRestAction(restAction, randomness)
        pairs.forEach {
            bindValues(it, doBuildBindingGene)
        }
    }

    /**
     * @return a list of pairs of genes to be bound within a [restAction]
     */
    fun buildBindingPairsInRestAction(restAction: RestCallAction, randomness: Randomness?): List<Pair<Gene, Gene>>{
        val pair = mutableListOf<Pair<Gene, Gene>>()
        val params = restAction.parameters
        val path = restAction.path

        if(ParamUtil.existBodyParam(params)){
            params.filterIsInstance<BodyParam>().forEach { bp->
                pair.addAll(buildBindBetweenParams(bp, path, path, params.filter { p -> p !is BodyParam }, true, randomness = randomness))
            }
        }
        params.forEach { p->
            params.find { sp -> sp != p && p.name == sp.name && p::class.java.simpleName == sp::class.java.simpleName }?.also {sp->
                pair.addAll(buildBindBetweenParams(sp, path, path, mutableListOf(p), randomness = randomness))
            }
        }
        return pair
    }

    /**
     * bind param of rest action based on [params] on the [sourcePath]
     * @param target is the param to be bound
     * @param targetPath is the path of the rest action
     * @param params is the source of binding
     * @param sourcePath is the source path of the rest action which contains [params]
     * @param doBuildBindingGene specified whether to build binding genes
     */
    fun bindRestAction(target : Param, targetPath: RestPath, sourcePath: RestPath, params: List<Param>, doBuildBindingGene: Boolean = false, randomness: Randomness?): Boolean{
        val pairs = buildBindBetweenParams(target, targetPath, sourcePath, params, false, randomness)
        pairs.forEach { p->
            bindValues(p, doBuildBindingGene)
        }
        return pairs.isNotEmpty()
    }

    private fun bindValues(p: Pair<Gene,Gene>, doBuildBindingGene: Boolean){
        val ok = p.first.setFromDifferentGene(p.second)
        if (ok && doBuildBindingGene){
            p.first.addBindingGene(p.second)
            p.second.addBindingGene(p.first)
        }

        val first = ParamUtil.getValueGene(p.first)
        val second = ParamUtil.getValueGene(p.second)
        if(ok && !doBuildBindingGene && first is StringGene && TaintInputName.isTaintInput(first.value)){
            //do not use same tainted value in non-bound genes
            if(second is StringGene){
                second.value = TaintInputName.getTaintName(StaticCounter.getAndIncrease())
            } else {
                //can this happen?
                log.warn("Possible issue in dealing with uniqueness of tainted values. Gene type: ${p.second.javaClass}")
            }
        }

    }

    /**
     *  bind [restAction] with [sqlActions]
     *  @param restAction is the action
     *  @param sqlActions specified the dbactions
     *  @param forceBindParamBasedOnDB specified whether to force to bind values of params in rest actions based on dbactions
     *  @param dbRemovedDueToRepair specified whether any db action is removed due to repair process.
     *          Note that dbactions might be truncated in the db repair process, thus the table related to rest actions might be removed.
     *  @param bindWith specified a list of resource call which might be bound with [this]
     */
    fun bindRestAndDbAction(restAction: RestCallAction,
                            restNode: RestResourceNode,
                            paramGeneBindMap: List<ParamGeneBindMap>,
                            sqlActions: List<SqlAction>,
                            forceBindParamBasedOnDB: Boolean = false,
                            dbRemovedDueToRepair : Boolean,
                            doBuildBindingGene: Boolean){
        buildBindRestActionBasedOnDbActions(restAction, restNode, paramGeneBindMap, sqlActions, forceBindParamBasedOnDB, dbRemovedDueToRepair).forEach { p->
            bindValues(p, doBuildBindingGene)
        }
    }

    /**
     * @return possible a list of pair genes which might be bounded with each other.
     *      Note that for each pair, the value of first is bound based on the value of second
     * @param target bind [target] based on other params, i.e., [params]
     * @param targetPath is the path of [target]
     * @param sourcePath of the [params]
     * @param params are used to bind with [target]
     */
    fun buildBindBetweenParams(target : Param, targetPath: RestPath, sourcePath: RestPath, params: List<Param>, doContain : Boolean = false, randomness: Randomness?) : List<Pair<Gene, Gene>>{
        return when(target){
            is BodyParam -> buildBindBodyParam(target, targetPath, sourcePath, params, doContain, randomness)
            is PathParam -> buildBindPathParm(target, targetPath, sourcePath, params, doContain)
            is QueryParam -> buildBindQueryParm(target, targetPath, sourcePath, params, doContain)
            is FormParam -> buildBindFormParam(target, params)?.run { listOf(this) }?: listOf()
            is HeaderParam -> buildBindHeaderParam(target, params)?.run { listOf(this) }?: listOf()
            else -> {
                LoggingUtil.uniqueWarn(log, "do not support gene binding for ${target::class.java.simpleName}")
                emptyList()
            }
        }
    }

    /**
     * @return whether the param name represents an extra param handled by taint analysis
     */
    fun isExtraTaintParam(name : String) : Boolean{
        return name == TaintInputName.EXTRA_HEADER_TAINT || name == TaintInputName.EXTRA_PARAM_TAINT
    }

    private fun buildBindHeaderParam(p : HeaderParam, params: List<Param>): Pair<Gene, Gene>?{
        return params.find { it is HeaderParam && p.name == it.name}?.run {
            Pair(ParamUtil.getValueGene(p.gene), ParamUtil.getValueGene(this.gene))
        }
    }

    private fun buildBindFormParam(p : FormParam, params: List<Param>): Pair<Gene, Gene>?{
        return params.find { it is FormParam && p.name == it.name}?.run {
            Pair(ParamUtil.getValueGene(p.gene), ParamUtil.getValueGene(this.gene))
        }
    }

    private fun buildBindQueryParm(p : QueryParam, targetPath: RestPath, sourcePath: RestPath, params: List<Param>, inner : Boolean): List<Pair<Gene, Gene>>{
        if(params.isNotEmpty() && ParamUtil.numOfBodyParam(params) == params.size){
            return buildBindBodyAndOther(params.first { pa -> pa is BodyParam } as BodyParam,
                sourcePath,
                p,
                targetPath,
                false,
                inner)
        }else{
            val sg = params.filter { pa -> !(pa is BodyParam) }.find { pa -> pa.name == p.name }
            if(sg != null){
                return listOf(Pair(ParamUtil.getValueGene(p.gene), ParamUtil.getValueGene(sg.gene)))
            }
        }
        return emptyList()
    }

    private fun buildBindBodyParam(bp : BodyParam, targetPath: RestPath, sourcePath: RestPath, params: List<Param>, inner : Boolean, randomness: Randomness?) : List<Pair<Gene, Gene>>{
        val excludeExtraParams = params.filterNot { isExtraTaintParam(it.name) }

        if(ParamUtil.numOfBodyParam(excludeExtraParams) != excludeExtraParams.size ){
            return excludeExtraParams.filter { p -> p !is BodyParam }
                .flatMap {ip->
                    buildBindBodyAndOther(bp, targetPath, ip, sourcePath, true, inner)
                }
        }else if(excludeExtraParams.isNotEmpty()){
            val valueGene = ParamUtil.getValueGene(bp.gene)
            val pValueGene = ParamUtil.getValueGene(excludeExtraParams[0].gene)
            if(valueGene !is ObjectGene){
                return listOf()
            }
            if (pValueGene !is ObjectGene){
                val field = valueGene.fields.find {
                    it::class.java.simpleName == pValueGene::class.java.simpleName && (it.name.equals(pValueGene.name, ignoreCase = true) || StringSimilarityComparator.isSimilar(
                        ParamUtil.modifyFieldName(valueGene, it), pValueGene.name))
                }?: return listOf()
                return listOf(Pair(field, pValueGene))
            }

            return buildBindObjectGeneWithObjectGene(valueGene, pValueGene, randomness)
        }
        return emptyList()
    }

    private fun buildBindObjectGeneWithObjectGene(b : ObjectGene, g : ObjectGene, randomness: Randomness?) : List<Pair<Gene, Gene>>{
        val map = mutableListOf<Pair<Gene, Gene>>()
        b.fields.forEach { f->
            val bound = f !is OptionalGene || f.isActive || (randomness == null || randomness.nextBoolean(PROBABILITY_BINDING_OBJECT))//(Math.random() < 0.5)
            if (bound){
                val mf = ParamUtil.getValueGene(f)
                val mName = ParamUtil.modifyFieldName(b, mf)
                val found = g.fields.find {ot->
                    val mot = ParamUtil.getValueGene(ot)
                    val pMName = ParamUtil.modifyFieldName(g, mot)
                    mf::class.java.simpleName == mot::class.java.simpleName && (pMName.equals(mName, ignoreCase = true) || StringSimilarityComparator.isSimilar(mName,pMName) )
                }
                if(found != null){
                    if (found is ObjectGene)
                        map.addAll(buildBindObjectGeneWithObjectGene(mf as ObjectGene, found, randomness))
                    else{
                        // FIXME, binding point
                        val vg = ParamUtil.getValueGene(found)
                        map.add(Pair(mf, vg))
                    }
                }
            }
        }
        return map
    }

    private fun buildBindPathParm(p : PathParam, targetPath: RestPath, sourcePath: RestPath, params: List<Param>, inner : Boolean): List<Pair<Gene, Gene>>{
        val k = params.find { pa -> pa is PathParam && pa.name == p.name }
        if(k != null){
            val mp = ParamUtil.getValueGene(p.gene)
            val mk = ParamUtil.getValueGene(k.gene)
            if (mp::class.java.simpleName == mk::class.java.simpleName){
                return listOf(Pair(mp, mk))
            } else{
                return listOf(buildBindingGene(mp, mk, true))
            }
        }
        else{
            if(ParamUtil.numOfBodyParam(params) == params.size && params.isNotEmpty()){
                return buildBindBodyAndOther(params.first { pa -> pa is BodyParam } as BodyParam,
                    sourcePath,
                    p,
                    targetPath,
                    false,
                    inner)
            }
        }
        return emptyList()
    }

    private fun buildBindBodyAndOther(body : BodyParam, bodyPath: RestPath, other : Param, otherPath : RestPath, b2g: Boolean, inner : Boolean): List<Pair<Gene, Gene>>{
        val otherGene = ParamUtil.getValueGene(other.gene)
        if (!ParamUtil.isGeneralName(otherGene.name)){
            val f = ParamUtil.getValueGene(body.gene).run {
                if (this is ObjectGene){
                    fields.find { f->
                        ParamUtil.compareField(f.name, refType, otherGene.name)
                    }
                }else
                    null
            }
            if (f != null && f::class.java.simpleName == otherGene::class.java.simpleName){
                return listOf(buildBindingGene(f, otherGene, b2g))
            }
        }

        val pathMap = ParamUtil.geneNameMaps(listOf(other), otherPath.getNonParameterTokens().reversed())
        val bodyMap = ParamUtil.geneNameMaps(listOf(body), bodyPath.getNonParameterTokens().reversed())

        return pathMap.mapNotNull { (pathkey, pathGene) ->
            if(bodyMap.containsKey(pathkey)){
                buildBindingGene(bodyMap.getValue(pathkey), pathGene, b2g)
            }else{
                val matched = bodyMap.keys.filter { s -> ParamUtil.scoreOfMatch(pathkey, s, inner) == 0 }
                if(matched.isNotEmpty()){
                    val first = matched.first()
                    buildBindingGene(bodyMap.getValue(first), pathGene, b2g)
                }else{
                    null
                }
            }
        }
    }

    /**
     * bind values between [restAction] and [sqlActions]
     * @param restAction is the action to be bounded with [sqlActions]
     * @param restNode is the resource node for the [restAction]
     * @param sqlActions are the dbactions generated for the [call]
     * @param bindingMap presents how to map the [restAction] and [sqlActions] at Gene-level
     * @param forceBindParamBasedOnDB specifies whether to bind params based on [sqlActions] or reversed
     * @param dbRemovedDueToRepair indicates whether the dbactions are removed due to repair.
     */
    fun buildBindRestActionBasedOnDbActions(restAction: RestCallAction,
                                            restNode: RestResourceNode,
                                            paramGeneBindMap: List<ParamGeneBindMap>,
                                            sqlActions: List<SqlAction>,
                                            forceBindParamBasedOnDB: Boolean = false,
                                            dbRemovedDueToRepair : Boolean) : List<Pair<Gene, Gene>>{

        val map = mutableListOf<Pair<Gene, Gene>>()

        Lazy.assert {
            paramGeneBindMap.isNotEmpty()
        }

        paramGeneBindMap.forEach { pToGene ->
            val dbAction = SqlActionUtils.findDbActionsByTableName(sqlActions, pToGene.tableName).firstOrNull()
            //there might due to a repair for dbactions
            if (dbAction == null && !dbRemovedDueToRepair)
                log.warn("cannot find ${pToGene.tableName} in db actions ${
                    sqlActions.joinToString(";") { it.table.name }
                }")
            if(dbAction != null){
                // columngene might be null if the column is nullable
                val columngene = findGeneBasedNameAndType(dbAction.seeTopGenes(), pToGene.column, type = null).firstOrNull()
                if (columngene != null){
                    val param = restAction.parameters.find { p -> restNode.getParamId(restAction.parameters, p)
                        .equals(pToGene.paramId, ignoreCase = true) }
                    if(param!= null){
                        if (pToGene.isElementOfParam && param is BodyParam) {

                            val objGene = ParamUtil.getValueGene(param.gene)
                            if (objGene is ObjectGene){
                                objGene.fields.find { f -> f.name == pToGene.targetToBind }?.let { paramGene ->
                                    map.add(buildBindingParamsWithDbAction(columngene, paramGene, forceBindParamBasedOnDB || dbAction.representExistingData))
                                }
                            }
                        } else {
                            map.add(buildBindingParamsWithDbAction(columngene, param.gene, forceBindParamBasedOnDB || dbAction.representExistingData))
                        }
                    }
                }
            }
        }

        return map
    }

    private fun findGeneBasedNameAndType(genes : List<Gene>, name: String?, type: String?) : List<Gene>{
        if (name == null && type == null)
            throw IllegalArgumentException("cannot find the gene with 'null' name and 'null' type")
        return genes.filter { g->
            (name?.equals(g.name, ignoreCase = true)?:true) && (type?.equals(g::class.java.simpleName)?:true)
        }
    }


    /**
     * derive a binding map between [dbgene] and [paramGene]
     */
    fun buildBindingParamsWithDbAction(dbgene: Gene, paramGene: Gene, existingData: Boolean, enableFlexibleBind : Boolean = true): Pair<Gene, Gene>{
        return if(dbgene is SqlPrimaryKeyGene || dbgene is SqlForeignKeyGene || dbgene is SqlAutoIncrementGene){
            /*
                if gene of dbaction is PK, FK or AutoIncrementGene,
                    bind gene of Param according to the gene from dbaction
             */
             buildBindingGene(b = ParamUtil.getValueGene(dbgene), g = ParamUtil.getValueGene(paramGene), b2g = false)
        }else{
            val db2Action = !existingData && (!enableFlexibleBind ||
                    checkBindSequence(ParamUtil.getValueGene(dbgene), ParamUtil.getValueGene(paramGene)) ?:true)
            buildBindingGene(
                b = ParamUtil.getValueGene(dbgene),
                g = ParamUtil.getValueGene(paramGene),
                b2g = db2Action
            )
        }
    }

    private fun buildBindingGene(b : Gene, g : Gene, b2g :Boolean): Pair<Gene, Gene>{
        return if(b::class.java.simpleName == g::class.java.simpleName){
            if (b2g) Pair(b,g)
            else Pair(g,b)
        }else if(b2g && (g is SqlPrimaryKeyGene || g is ImmutableDataHolderGene || g is SqlForeignKeyGene || g is SqlAutoIncrementGene)){
            Pair(b,g)
        }else if(!b2g && (b is SqlPrimaryKeyGene || b is ImmutableDataHolderGene || b is SqlForeignKeyGene || b is SqlAutoIncrementGene))
            Pair(g,b)
        else{
            if(b2g) Pair(b,g)
            else Pair(g,b)
        }
    }

    /**
     * [geneA] copy values from [geneB]
     * @return null cannot find its priority
     *         true keep current sequence
     *         false change current sequence
     */
    private fun checkBindSequence(geneA: Gene, geneB: Gene) : Boolean?{
        val pA = getGeneTypePriority(geneA)
        val pB = getGeneTypePriority(geneB)

        if(pA == -1 || pB == -1) return null

        if(pA >= pB) return true

        return false
    }

    private val GENETYPE_BINDING_PRIORITY = mapOf<Int, Set<String>>(
        (0 to setOf(SqlPrimaryKeyGene::class.java.simpleName, SqlAutoIncrementGene::class.java.simpleName, SqlForeignKeyGene::class.java.simpleName, ImmutableDataHolderGene::class.java.simpleName)),
        (1 to setOf(DateTimeGene::class.java.simpleName, DateGene::class.java.simpleName, TimeGene::class.java.simpleName)),
        (2 to setOf(Boolean::class.java.simpleName)),
        (3 to setOf(IntegerGene::class.java.simpleName)),
        (4 to setOf(LongGene::class.java.simpleName)),
        (5 to setOf(FloatGene::class.java.simpleName)),
        (6 to setOf(DoubleGene::class.java.simpleName)),
        (7 to setOf(ArrayGene::class.java.simpleName, ObjectGene::class.java.simpleName, EnumGene::class.java.simpleName, CycleObjectGene::class.java.simpleName, FixedMapGene::class.java.simpleName)),
        (8 to setOf(StringGene::class.java.simpleName, Base64StringGene::class.java.simpleName))
    )

    private fun getGeneTypePriority(gene: Gene) : Int{
        val typeName = gene::class.java.simpleName
        GENETYPE_BINDING_PRIORITY.filter { it.value.contains(typeName) }.let {
            return if(it.isEmpty()) -1 else it.keys.first()
        }
    }
}
