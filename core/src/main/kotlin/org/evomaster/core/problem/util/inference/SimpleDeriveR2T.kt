package org.evomaster.core.problem.util.inference

import org.evomaster.core.sql.SqlAction
import org.evomaster.core.sql.schema.Table
import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest.param.BodyParam
import org.evomaster.core.problem.rest.resource.ParamInfo
import org.evomaster.core.problem.rest.resource.RestResourceCalls
import org.evomaster.core.problem.rest.resource.RestResourceNode
import org.evomaster.core.problem.rest.resource.dependency.*
import org.evomaster.core.problem.util.ParamUtil
import org.evomaster.core.problem.util.inference.model.MatchedInfo
import org.evomaster.core.problem.util.inference.model.ParamGeneBindMap
import org.evomaster.core.problem.util.StringSimilarityComparator
import org.evomaster.core.problem.util.StringSimilarityComparator.stringSimilarityScore
import org.evomaster.core.search.action.ActionFilter
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.sql.SqlActionUtils.SCHEMA_TABLE_SEPARATOR
import org.evomaster.core.sql.schema.TableId
import kotlin.math.max

/**
 * process inference related to resource
 */
object SimpleDeriveResourceBinding : DeriveResourceBinding {

    /*************************** resource to table and param to table *****************************/

    /**
     * derive relationship between resource and tables,
     *          i.e., the action under the resource may manipulate data from the table
     * e.g., /A/{a}/B{b}, the resource is related to two resource A and B
     */
    override fun deriveResourceToTable(resourceCluster: MutableList<RestResourceNode>, allTables : Map<TableId, Table>){
        resourceCluster.forEach { r->
            deriveResourceToTable(r, allTables)
        }
    }

    /**
     * derive a relationship between a resource and a table
     */
    override fun deriveResourceToTable(resourceNode: RestResourceNode, allTables : Map<TableId, Table>){
        //1. derive resource to table

        //1.1 derive resource to tables based on segments
        if(allTables.isNotEmpty()){
            resourceNode.getAllSegments(flatten = true).forEach { seg ->
                ParamUtil.parseParams(seg).forEachIndexed stop@{ sindex, token ->
                    /*
                        check whether any table name matches token
                        table name might contain schema,
                     */
                    val matchedMap = allTables.keys.map {
                        Pair(it, calculateStringSimilarityScoreWithTableName(it.getFullQualifyingTableName(), token))
                    }.asSequence().sortedBy { e->e.second }

                    if(matchedMap.lastOrNull()!= null && matchedMap.last().second >= StringSimilarityComparator.SimilarityThreshold){
                        matchedMap.filter { it.second == matchedMap.last().second }.forEach {
                            resourceNode.resourceToTable.derivedMap.getOrPut(it.first){
                                mutableListOf()
                            }.add(MatchedInfo(seg, it.first.getFullQualifyingTableName(), similarity = it.second, inputIndicator = sindex, outputIndicator = 0))
                        }
                        return@stop
                    }

                    val matchedPropertyMap = allTables
                        .flatMap { t->t.value.columns
                            .filter { c-> !ParamUtil.isGeneralName(c.name) }
                            .map { c->Pair(t.value.id, c.name) }
                        }
                        .map { p-> Pair(p.first,
                            Pair(p.second, calculateStringSimilarityScoreWithTableName(p.second, token)
                        )) }.asSequence().sortedBy { e->e.second.second }

                    if(matchedPropertyMap.lastOrNull() != null && matchedPropertyMap.last().second.second >= StringSimilarityComparator.SimilarityThreshold){
                        matchedPropertyMap.filter { it.second.second == matchedPropertyMap.last().second.second }.forEach {
                            resourceNode.resourceToTable.derivedMap.getOrPut(it.first){
                                mutableListOf()
                            }.add(MatchedInfo(seg, it.first.getFullQualifyingTableName(), similarity = it.second.second, inputIndicator = sindex, outputIndicator = 1))
                        }
                        return@stop
                    }
                }
            }
        }

        //1.2 derive resource to tables based on type
        val reftypes = resourceNode.actions.filter { it.parameters.any{ p-> p is BodyParam && p.gene is ObjectGene && p.gene.refType != null}}
                .flatMap { (it as RestCallAction ).parameters.filter{p-> p is BodyParam && p.gene is ObjectGene && p.gene.refType != null}.map { p-> (p.gene as ObjectGene).refType!!}}

        if(reftypes.isNotEmpty()){
            reftypes.forEach { type->
                if(!resourceNode.isPartOfStaticTokens(type) && allTables.isNotEmpty()){
                    val matchedMap = allTables.keys.map { Pair(it, calculateStringSimilarityScoreWithTableName(it.getFullQualifyingTableName(), type)) }.asSequence().sortedBy { e->e.second }
                    if(matchedMap.last().second >= StringSimilarityComparator.SimilarityThreshold){
                        matchedMap.filter { it.second == matchedMap.last().second }.forEach {
                            resourceNode.resourceToTable.derivedMap.getOrPut(it.first){
                                mutableListOf()
                            }.add(MatchedInfo(type, it.first.getFullQualifyingTableName(), similarity = it.second, inputIndicator = 0, outputIndicator = 0))
                        }
                    }
                }
            }
        }
        //1.3 derive resource to tables based on tokens on POST action
        resourceNode.actions.filter {it.verb == HttpVerb.POST }.forEach { post->
            post.tokens.values.filter { !resourceNode.getName().toLowerCase().contains(it.getKey().toLowerCase()) }.forEach { atoken->
                val matchedMap = allTables.keys.map { Pair(it, calculateStringSimilarityScoreWithTableName(it.getFullQualifyingTableName(), atoken.getKey())) }.asSequence().sortedBy { e->e.second }
                matchedMap.last().apply {
                    if(second >= StringSimilarityComparator.SimilarityThreshold){
                        resourceNode.resourceToTable.derivedMap.getOrPut(first){
                            mutableListOf()
                        }.add(MatchedInfo(atoken.getKey(), first.getFullQualifyingTableName(), similarity = second, inputIndicator = 1, outputIndicator = 0))
                    }
                }
            }
        }

        //2. derive params to the tables
        deriveParamsToTable(resourceNode.paramsInfo, resourceNode, allTables)
    }

    /**
     * compare [content] with given [tableName]
     * tableName can contain info of schema,
     * in this case we also calculate similarity with the last segment
     * then return the higher similarity
     */
    private fun calculateStringSimilarityScoreWithTableName(tableName: String, content: String): Double{
        var score = stringSimilarityScore(tableName, content) // full A vs full B
        val shortContent = if (content.contains(SCHEMA_TABLE_SEPARATOR)) content.split(SCHEMA_TABLE_SEPARATOR).last() else null
        val shortTable = if (tableName.contains(SCHEMA_TABLE_SEPARATOR)) tableName.split(SCHEMA_TABLE_SEPARATOR).last() else null
        if (shortContent == null && shortTable == null) return score

        if (shortContent != null)
            score = max(score, stringSimilarityScore(tableName, shortContent)) // full A vs short B
        if (shortTable != null)
            score = max(score, stringSimilarityScore(shortTable, content)) // short A vs full B
        if (shortTable != null && shortContent != null)
            score = max(score, stringSimilarityScore(shortTable, shortContent))
        return score
    }

    fun deriveParamsToTable(mapParamInfo : Map<String, ParamInfo>, r: RestResourceNode, allTables : Map<TableId, Table>){
        mapParamInfo.forEach { (paramId, paramInfo) ->
            deriveParamsToTable(paramId, paramInfo, r, allTables)
        }
    }

    fun deriveParamsToTable(paramId : String, paramInfo : ParamInfo, r: RestResourceNode, allTables : Map<TableId, Table>){

        val inputIndicator = paramInfo.segmentLevel

        val relatedTables = if(r.path.getVariableNames().size > 1)r.resourceToTable.derivedMap.filter { it.value.any { m-> m.input == paramInfo.preSegment } }.keys.toHashSet()
            else r.resourceToTable.derivedMap.keys.toHashSet()

        val isBodyParam = (paramInfo.referParam is BodyParam) && (ParamUtil.getObjectGene(paramInfo.referParam.gene).run {
            this != null && this.fields.isNotEmpty()
        })

        var created = false
        if(isBodyParam){
            var tables = if(ParamUtil.getObjectGene(paramInfo.referParam.gene)?.refType != null){
                r.resourceToTable.derivedMap.filter { it.value.any { m-> m.input == (ParamUtil.getObjectGene(paramInfo.referParam.gene))!!.refType!!} }.keys.toHashSet()
            } else null

            if(tables.isNullOrEmpty()) tables = relatedTables
            created = deriveRelatedTable(r, paramId, paramInfo, tables, isBodyParam, inputIndicator, alltables = allTables )
        }

        if(!created){
            created = deriveRelatedTable(r, paramId, paramInfo, relatedTables, false, inputIndicator, alltables = allTables)
        }
    }

    fun deriveRelatedTable(
        r : RestResourceNode,
        paramId: String,
        paramInfo: ParamInfo,
        relatedToTables: Set<TableId>,
        isBodyParam : Boolean,
        inputIndicator: Int,
        alltables : Map<TableId, Table>
    ) : Boolean{
        if(isBodyParam){
            val pToTable = BodyParamRelatedToTable(paramId, paramInfo.referParam)
            ParamUtil.getObjectGene(paramInfo.referParam.gene)?.fields?.forEach { f->
                val matchedMap : MutableMap<String, MatchedInfo> = mutableMapOf()
                deriveParamWithTable(f.name, relatedToTables, matchedMap, inputIndicator, alltables)
                if(matchedMap.isNotEmpty()){
                    val fToTable = ParamFieldRelatedToTable(f.name)
                    fToTable.derivedMap.putAll(matchedMap)
                    pToTable.fieldsMap.putIfAbsent(f.name, fToTable)
                }
            }
            if(pToTable.fieldsMap.isNotEmpty()) {
                r.resourceToTable.paramToTable.putIfAbsent(paramId, pToTable)
                return true
            }
        }else{
            val matchedMap : MutableMap<String, MatchedInfo> = mutableMapOf()
            deriveParamWithTable(paramInfo.name, relatedToTables, matchedMap, inputIndicator, alltables)
            if(matchedMap.isNotEmpty()){
                val pToTable = SimpleParamRelatedToTable(paramId, paramInfo.referParam)
                pToTable.derivedMap.putAll(matchedMap)
                r.resourceToTable.paramToTable.putIfAbsent(paramId, pToTable)
                return true
            }
        }
        return false
    }


    /**
     * derive related tables of parameters
     *
     * Note that same parameter (e.g., id) may related to multiple related tables.
     */
    private fun deriveParamWithTable(
        paramName : String,
        candidateTables : Set<TableId>,
        pToTable : MutableMap<String, MatchedInfo>,
        inputlevel: Int,
        tables : Map<TableId, Table>){
        candidateTables.forEach { tableName ->
            deriveParamWithTable(paramName, tableName, pToTable, inputlevel, tables)
        }
    }

    private fun deriveParamWithTable(
        paramName : String,
        tableName: TableId,
        pToTable : MutableMap<String, MatchedInfo>,
        inputlevel: Int,
        tables : Map<TableId, Table>
    ){
        /*
            paramName might be \w+id or \w+name, in this case, we compare paramName with table name of current or referred table + column name
         */
        //getTable(tableName, tables)?
        tables[tableName]?.let { t->
            val matchedMap = t.columns
                    .map {
                        Pair(it.name,
                                if(ParamUtil.isGeneralName(it.name))
                                    t.foreignKeys.map { f-> calculateStringSimilarityScoreWithTableName(paramName, "${f.targetTableId}${it.name}")}
                                            .plus(calculateStringSimilarityScoreWithTableName(paramName, it.name))
                                            .plus(calculateStringSimilarityScoreWithTableName(paramName, "$tableName${it.name}"))
                                            .asSequence().sorted().last()
                                else if(ParamUtil.isGeneralName(paramName))
                                    t.foreignKeys.map { f-> calculateStringSimilarityScoreWithTableName("${f.targetTableId}$paramName", it.name)}
                                            .plus(calculateStringSimilarityScoreWithTableName(paramName, it.name))
                                            .plus(calculateStringSimilarityScoreWithTableName("$tableName$paramName", it.name))
                                            .asSequence().sorted().last()
                                else calculateStringSimilarityScoreWithTableName(paramName, it.name))
                    }.asSequence().sortedBy { e->e.second }
            if(matchedMap.last().second >= StringSimilarityComparator.SimilarityThreshold){
                matchedMap.filter { it.second == matchedMap.last().second }.forEach {
                    pToTable.getOrPut(tableName){
                        MatchedInfo(paramName, it.first, similarity = it.second, inputIndicator = inputlevel, outputIndicator = 1)
                    }
                }
            }
        }
    }

    /**
     * @return a bining map between [calls] and its related tables
     *
     * @param paramsInfo of calls to be bound
     * @param calls to be bound
     * @param sqlActions specifies the tables to be analyzed.
     *          if [dbActions] is empty, the tables would be all related tables extracted from its resource node
     */
    override fun generateRelatedTables(
        paramsInfo: List<ParamInfo>,
        calls: RestResourceCalls,
        sqlActions : List<SqlAction>
    ): MutableMap<RestCallAction, MutableList<ParamGeneBindMap>> {

        val result = mutableMapOf<RestCallAction, MutableList<ParamGeneBindMap>>()

        val missingParams = paramsInfo.map { it.key }
        val resource = calls.getResourceNode()

        val relatedTables = sqlActions.map { it.table.name }.toHashSet()

        val list = if(relatedTables.isEmpty())
            getBindMap(missingParams.toSet(), resource.resourceToTable)
        else
            getBindMap(missingParams.toSet(), resource.resourceToTable, relatedTables)

        if(list.isNotEmpty()){
            val cleanList = mutableListOf<ParamGeneBindMap>()
            list.forEach { p->
                if(!cleanList.any { e->e.equalWith(p)}) cleanList.add(p)
            }
            calls.seeActions(ActionFilter.NO_SQL).filter { it is RestCallAction  && it.path.toString() == resource.getName()}.forEach { a->
                result[a as RestCallAction] = cleanList.filter { p-> (paramsInfo.any { m-> m.key == p.paramId && m.involvedAction.contains(a.verb) })}.toMutableList()
            }
        }
        return result
    }

    private fun getBindMap(paramIds : Set<String>, resourceToTable: ResourceRelatedToTable) : MutableList<ParamGeneBindMap>{
        val result = mutableListOf<ParamGeneBindMap>()
        paramIds.forEach { p->
            resourceToTable.paramToTable[p]?.let {pToTable->
                var tables = resourceToTable.getConfirmedDirectTables()
                var found = false
                if(tables.isNotEmpty()){
                    found = getBindMap(p, pToTable, tables, resourceToTable, result)
                }
                tables = resourceToTable.getTablesInDerivedMap()
                if(tables.isNotEmpty())
                    found = getBindMap(p, pToTable, tables, resourceToTable, result)

                if(!found){
                    //cannot bind this paramid with table
                }
            }
        }
        return result
    }

    private fun getBindMap(paramIds : Set<String>, resourceToTable: ResourceRelatedToTable, tables: Set<String>) : MutableList<ParamGeneBindMap>{
        val result = mutableListOf<ParamGeneBindMap>()
        paramIds.forEach { p->
            resourceToTable.paramToTable[p]?.let {pToTable->
                getBindMap(p, pToTable, tables, resourceToTable, result)
            }
        }
        return result
    }


    private fun getBindMap(paramId: String, pToTable : ParamRelatedToTable, tables : Set<TableId>, resourceToTable: ResourceRelatedToTable, result :  MutableList<ParamGeneBindMap>) : Boolean{
        if(pToTable is SimpleParamRelatedToTable){
            resourceToTable.findBestTableForParam(tables, pToTable)?.let {pair->
                val target = pair.first.toList()[(0..(pair.first.size-1)).shuffled().first()]
                val column = resourceToTable.getSimpleParamToSpecifiedTable(target, pToTable)!!.second
                result.add(ParamGeneBindMap(paramId, false, pToTable.referParam.name, tableName = target, column = column))
                return true
            }
        }else if(pToTable is BodyParamRelatedToTable){
            resourceToTable.findBestTableForParam(tables, pToTable)?.let {pair->
                val vote = pair.values.flatMap { it.first }.toMutableSet().map { Pair(it, 0) }.toMap().toMutableMap()

                pair.forEach { (f, bestSet) ->
                    bestSet.first.forEach { t->
                        vote.replace(t, vote[t]!!+1)
                    }
                }

                pair.forEach { (f, bestSet) ->
                    val target = if (bestSet.first.size == 1) bestSet.first.first() else bestSet.first.asSequence().sortedBy { vote[it] }.last()
                    val column = resourceToTable.getBodyParamToSpecifiedTable(target, pToTable, f)!!.second.second
                    result.add(ParamGeneBindMap(paramId, true, f, tableName = target, column = column))
                }

                return true
            }
        }
        return false
    }

    /*************************** resource to table and param to table *****************************/
    private fun getTable(tableName: String, tables : Map<String, Table>) : Table?{
        return tables.values.find{ it.name.equals(tableName, ignoreCase = true)}
    }
}