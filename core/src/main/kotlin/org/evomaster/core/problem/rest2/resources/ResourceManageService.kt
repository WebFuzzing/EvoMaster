package org.evomaster.core.problem.rest2.resources

import com.google.inject.Inject
import org.evomaster.client.java.controller.api.dto.database.operations.DataRowDto
import org.evomaster.core.EMConfig
import org.evomaster.core.database.DbAction
import org.evomaster.core.database.DbActionUtils
import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.core.problem.rest.RestAction
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest.SampleType
import org.evomaster.core.problem.rest.auth.AuthenticationInfo
import org.evomaster.core.problem.rest.param.BodyParam
import org.evomaster.core.problem.rest.param.PathParam
import org.evomaster.core.problem.rest.param.QueryParam
import org.evomaster.core.problem.rest.serviceII.RTemplateHandler
import org.evomaster.core.problem.rest.serviceII.ParamHandler
import org.evomaster.core.problem.rest.serviceII.RestIndividualII
import org.evomaster.core.problem.rest.serviceII.RestSamplerII
import org.evomaster.core.problem.rest.serviceII.resources.RestAResource
import org.evomaster.core.problem.rest.serviceII.resources.RestResourceCalls
import org.evomaster.core.problem.rest2.resources.dependency.ParamRelatedToTable
import org.evomaster.core.problem.rest2.resources.dependency.PossibleCreationChain
import org.evomaster.core.problem.rest2.resources.token.parser.ParserUtil
import org.evomaster.core.search.Action
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.Sampler
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ResourceManageService {

    companion object {
        val log: Logger = LoggerFactory.getLogger(ResourceManageService::class.java)
    }

    @Inject
    private lateinit var sampler: Sampler<*>

    @Inject
    private lateinit var randomness: Randomness

    @Inject
    private lateinit var config: EMConfig


    private var initialized = false
    /**
     * key is resource path
     * value is an abstract resource
     */
    private val resourceCluster : MutableMap<String, RestAResource> = mutableMapOf()
    /**
     * key is resource path
     * value is a list of tables that are related to the resource
     */
    private val resourceTables : MutableMap<String, MutableSet<String>> = mutableMapOf()

    /**
     * key is table name
     * value is a list of existing data of PKs in DB
     */
    private var dataInDB : MutableMap<String, MutableList<DataRowDto>> = mutableMapOf()

    private val missingCreation : MutableMap<String, MutableList<PossibleCreationChain>> = mutableMapOf()

    fun initAbstractResources(actionCluster : MutableMap<String, Action>) {
        if (!initialized) {
            actionCluster.values.forEach { u ->
                if (u is RestCallAction) {
                    val resource = resourceCluster.getOrPut(u.path.toString()) {
                        RestAResource(u.path.copy(), mutableListOf())
                    }
                    resource.actions.add(u)
                }
            }
            resourceCluster.values.forEach{it.initAncestors(getResourceCluster().values.toList())}

            resourceCluster.values.forEach{it.init()}

            resourceCluster.forEach { t, u ->
                if(!u.postCreation.isComplete()){
                    missingCreation.put(t, mutableListOf())
                }
            }

            if(hasDBHandler()){
                snapshotDB()
                /*
                    derive possible db creation for each abstract resources.
                    The derived db creation needs to be further confirmed based on feedback from evomaster driver (NOT IMPLEMENTED YET)
                 */
                resourceCluster.values.forEach {ar->
                    if(ar.paramsToTables.isEmpty())
                        deriveRelatedTables(ar,false)
                }
            }

            initialized = true
        }
    }

    /**
     * this function is used to initialized ad-hoc individuals
     */
    fun createAdHocIndividuals(auth: AuthenticationInfo, adHocInitialIndividuals : MutableList<RestIndividualII>){
        val sortedResources = resourceCluster.values.sortedByDescending { it.tokens.size }.asSequence()

        //GET, PATCH, DELETE
        sortedResources.forEach { ar->
            ar.actions.filter { it is RestCallAction && it.verb != HttpVerb.POST && it.verb != HttpVerb.PUT }.forEach {a->
                val call = ar.sampleOneAction(a.copy() as RestAction, randomness)
                call.actions.forEach {a->
                    if(a is RestCallAction) a.auth = auth
                }
                adHocInitialIndividuals.add(RestIndividualII(mutableListOf(call), SampleType.SMART_RESOURCE))
            }
        }

        //all POST with one post action
        sortedResources.forEach { ar->
            ar.actions.filter { it is RestCallAction && it.verb == HttpVerb.POST}.forEach { a->
                val call = ar.sampleOneAction(a.copy() as RestAction, randomness)
                call.actions.forEach { (it as RestCallAction).auth = auth }
                adHocInitialIndividuals.add(RestIndividualII(mutableListOf(call), SampleType.SMART_RESOURCE))
            }
        }

        sortedResources
                .filter { it.actions.find { a -> a is RestCallAction && a.verb == HttpVerb.POST } != null && it.postCreation.actions.size > 1   }
                .forEach { ar->
                    ar.genPostChain(randomness)?.let {call->
                        call.actions.forEach { (it as RestCallAction).auth = auth }
                        call.doesCompareDB = hasDBHandler()
                        adHocInitialIndividuals.add(RestIndividualII(mutableListOf(call), SampleType.SMART_RESOURCE))
                    }
                }

        //PUT
        sortedResources.forEach { ar->
            ar.actions.filter { it is RestCallAction && it.verb == HttpVerb.PUT }.forEach {a->
                val call = ar.sampleOneAction(a.copy() as RestAction, randomness)
                call.actions.forEach { (it as RestCallAction).auth = auth }
                adHocInitialIndividuals.add(RestIndividualII(mutableListOf(call), SampleType.SMART_RESOURCE))
            }
        }

        //template
        sortedResources.forEach { ar->
            ar.templates.values.filter { t-> t.template.contains(RTemplateHandler.SeparatorTemplate) }
                    .forEach {ct->
                        val call = ar.sampleRestResourceCalls(ct.template, randomness, config.maxTestSize)
                        call.actions.forEach { if(it is RestCallAction) it.auth = auth }
                        adHocInitialIndividuals.add(RestIndividualII(mutableListOf(call), SampleType.SMART_RESOURCE))
                    }
        }

    }


    fun sampleCall(resourceKey: String, doesCreateResource: Boolean, calls : MutableList<RestResourceCalls>, size : Int){
        val ar = resourceCluster[resourceKey]
                ?: throw IllegalArgumentException("resource path $resourceKey does not exist!")


        if(!doesCreateResource ){
            calls.add(ar.sampleIndResourceCall(randomness,size))
            return
        }

        assert(!ar.isIndependent())
        var candidateForInsertion : String? = null

        if(ar.paramsToTables.isNotEmpty() && randomness.nextBoolean(0.5)){
            //Insert - GET/PUT/PATCH
            val candidates = ar.templates.filter { it.value.independent }
            candidateForInsertion = if(candidates.isNotEmpty()) randomness.choose(candidates.keys) else null
        }


        val candidate = if(candidateForInsertion.isNullOrBlank())
            randomness.choose(ar.templates.keys) else candidateForInsertion

        val call = ar.genCalls(candidate,randomness,size,true,true)
        calls.add(call)

        if(hasDBHandler()){
            if(!ar.postCreation.isComplete() || checkIfDeriveTable(call) || candidateForInsertion != null){
                call.doesCompareDB = true
                /*
                    derive possible db, and bind value according to db
                */
                val created = handleCallWithDBAction(ar, call, false)
                if(!created){
                    //TODO MAN record the call when postCreation fails
                }
            }else{
                call.doesCompareDB = (!call.template.independent) && (resourceTables[ar.path.toString()] == null)
            }
        }
    }


//    fun generateCall(resourceKey: String, calls : MutableList<RestResourceCalls>, size : Int){
//        val ar = resourceCluster[resourceKey]
//                ?: throw IllegalArgumentException("resource path $resourceKey does not exist!")
//
//        val call = ar.sampleRestResourceCalls(randomness, size, hasDBHandler())
//        calls.add(call)
//        if(hasDBHandler()){
//            if(!ar.postCreation.isComplete() || checkIfDeriveTable(call)){
//                call.doesCompareDB = true
//                /*
//                    derive possible db, and bind value according to db
//                */
//                val created = deriveRelatedTables(ar, call)
//                if(!created){
//                    //TODO MAN record the call when postCreation fails
//                }else{
//                   //TODO manage independent of the template if(call.template.independent)
//                }
//            }else{
//                call.doesCompareDB = (!call.template.independent) && (resourceTables[ar.path.toString()] == null)
//            }
//        }
//    }

    private fun checkIfDeriveTable(call: RestResourceCalls) : Boolean{
        if(!call.template.independent) return false

        call.actions.first().apply {
            if (this is RestCallAction){
                if(this.parameters.isNotEmpty()) return true
            }
        }

        return false
    }

    private fun deriveRelatedTables(ar: RestAResource, startWithPostIfHas : Boolean = true){
        val post = ar.postCreation.actions.firstOrNull()
        val skip = if(startWithPostIfHas && post != null && (post as RestCallAction).path.isLastElementAParameter())  1 else 0

        val missingParams = mutableListOf<String>()
        var withParam = false

        ar.tokens.values.reversed().asSequence().forEachIndexed { index, pathRToken ->
            if(index >= skip){
                if(pathRToken.isParameter){
                    missingParams.add(0, pathRToken.getKey())
                    withParam = true
                }else if(withParam){
                    missingParams.set(0, pathRToken.getKey() + ParamHandler.separator+ missingParams[0] )
                }
            }
        }

        val lastToken = if(missingParams.isNotEmpty()) missingParams.last()
                        else if(ar.tokens.isNotEmpty()) ar.tokens.map { it.value.getKey() }.joinToString ( ParamHandler.separator )
                        else null
        ar.actions
                .filter { it is RestCallAction }
                .flatMap { (it as RestCallAction).parameters }
                .filter { it !is PathParam }
                .forEach { p->
                    when(p){
                        is BodyParam -> missingParams.add(
                                (if(lastToken!=null) lastToken+ParamHandler.separator else "") +
                                        (if(p.gene is ObjectGene && p.gene.refType != null && p.name.toLowerCase() != p.gene.refType.toLowerCase() )
                                                p.name+ParamHandler.separator+p.gene.refType else p.name)
                        )
                        is QueryParam -> missingParams.add((if(lastToken!=null) lastToken+ParamHandler.separator else "") + p.name)
                        else ->{
                            //do nothing
                        }
                    }
                }
        missingParams.forEach { pname->
            val params = pname.split(ParamHandler.separator)

            var similarity = 0.0
            var tableName = ""

            params.reversed().forEach findP@{
                dataInDB.forEach { t, u ->
                    val score = ParserUtil.stringSimilarityScore(it, t)
                    if(score > similarity){
                        similarity =score
                        tableName = t
                    }
                }
                if(similarity >= ParserUtil.SimilarityThreshold){
                    return@findP
                }
            }

            val p = params.last()
            val rt = ParamRelatedToTable(p, if(dataInDB[tableName] != null) mutableListOf(tableName) else mutableListOf(), similarity, pname)
            ar.paramsToTables.getOrPut(rt.notateKey()){
                rt
            }
        }
    }

    private fun handleCallWithDBAction(ar: RestAResource, call: RestResourceCalls, forceInsert : Boolean) : Boolean{

        if(ar.paramsToTables.values.find { it.probability < ParserUtil.SimilarityThreshold || it.targets.isEmpty()} == null){
            var failToLinkWithResource = false

            val paramsToBind = ar.actions.filter { (it is RestCallAction) && it.verb != HttpVerb.POST }.flatMap { (it as RestCallAction).parameters.map { p-> ParamRelatedToTable.getNotateKey(p.name.toLowerCase()).toLowerCase()  } }
            val targets = ar.paramsToTables.filter { paramsToBind.contains(it.key.toLowerCase())}
            snapshotDB()

            val tables = targets.map { it.value.targets.first().toString() }.toHashSet()

            tables.forEach { tableName->
                val ps = targets.filter { it.value.targets.first().toString() == tableName }.map { it.value.additionalInfo }.toHashSet().toList()

                if(forceInsert){
                    if(!handleCallWithInsert(tableName, ps, call)) failToLinkWithResource = true
                }else{
                    if(dataInDB[tableName]!= null ){
                        val size = dataInDB[tableName]!!.size
                        when{
                            size < config.minRowOfTable -> if(!handleCallWithInsert(tableName, ps, call)) failToLinkWithResource = true
                            else ->{
                                if(randomness.nextBoolean(config.probOfSelectFromDB)){
                                    if(!handleCallWithSelect(tableName, ps, call)) failToLinkWithResource = true
                                }else{
                                    if(!handleCallWithInsert(tableName, ps, call)) failToLinkWithResource = true
                                }
                            }
                        }
                    }else
                        failToLinkWithResource = true
                }
                tables.add(tableName)
            }
            return targets.isNotEmpty() && !failToLinkWithResource
        }
        return false
    }

    private fun deriveRelatedTables(ar: RestAResource, call : RestResourceCalls) : Boolean{

        if(ar.paramsToTables.values.find { it.probability < ParserUtil.SimilarityThreshold || it.targets.isEmpty()} == null){
            var empty = false

            snapshotDB()
            ar.paramsToTables.forEach { t, u ->
                val ps = u.additionalInfo.split(ParamHandler.separator)

                val tableName = u.targets.first().toString()

                if(dataInDB[tableName]!= null ){
                    val size = dataInDB[tableName]!!.size
                    when{
                        size < config.minRowOfTable -> if(!handleCallWithInsert(tableName, ps, call)) empty = true
                        else ->{
                            if(randomness.nextBoolean(config.probOfSelectFromDB)){
                                if(!handleCallWithSelect(tableName, ps, call)) empty = true
                            }else{
                                if(!handleCallWithInsert(tableName, ps, call)) empty = true
                            }
                        }
                    }

                }else
                    empty = true

            }

            return !empty
        }
        return false
    }


    fun repairRestResourceCalls(call: RestResourceCalls)  : Boolean{
        if(call.dbActions.isNotEmpty()){
            val key = call.resource.ar.path.toString()
            val ar = resourceCluster[key]
                    ?: throw IllegalArgumentException("resource path $key does not exist!")
            call.dbActions.clear()
            handleCallWithDBAction(ar, call, true)
        }
        return true
    }


    private fun handleCallWithInsert(tableName: String, ps: List<String>, call: RestResourceCalls, bindParamBasedOnDB: Boolean = false): Boolean{
        val insertDbAction =
                (sampler as RestSamplerII).sqlInsertBuilder!!
                        .createSqlInsertionActionWithRandomizedData(tableName)
        var ok : Boolean? = null
        if(insertDbAction.isNotEmpty()){
            DbActionUtils.randomizeDbActionGenes(insertDbAction, randomness)
            repairDbActions(insertDbAction.toMutableList())
            if(!bindParamBasedOnDB)
                bindCallActionsWithDBAction(ps, call, insertDbAction, bindParamBasedOnDB)
            //ok = (sampler as RestSamplerII).sqlInsertBuilder!!.executeInsertSql(insertDbAction)
            return true
        }
//        if(ok != null && ok){
//            snapshotDB()
//            if(bindParamBasedOnDB)
//                bindCallActionsWithDBAction(ps, call, insertDbAction, bindParamBasedOnDB)
//            return true
//        }else if(dataInDB[tableName]!= null && dataInDB[tableName]!!.size > 0){
//            return handleCallWithSelect(tableName, ps, call, true)
//        }
        return false
    }

    private fun handleCallWithSelect(tableName: String, ps: List<String>, call: RestResourceCalls, forceDifferent: Boolean = false) : Boolean{
        assert(dataInDB[tableName] != null && dataInDB[tableName]!!.size > 0)

        val columns = if(forceDifferent && call.dbActions.isNotEmpty() && call.dbActions.last().representExistingData)selectToDataRowDto(call.dbActions.last(), tableName)
                        else randomness.choose(dataInDB[tableName]!!)

        val selectDbAction = (sampler as RestSamplerII).sqlInsertBuilder!!.extractExistingByCols(tableName, columns)
        bindCallActionsWithDBAction(ps, call, listOf(selectDbAction), true)
        return true
    }

    private fun bindCallActionsWithDBAction(ps: List<String>, call: RestResourceCalls, dbActions : List<DbAction>, bindParamBasedOnDB : Boolean){
        call.dbActions.addAll(dbActions)

        ps.forEach { pname->
            val pss = pname.split(ParamHandler.separator)
            call.actions
                    .filter { (it is RestCallAction) && it.parameters.find { it.name.toLowerCase() == pss.last().toLowerCase() } != null }
                    .forEach { action->
                        (action as RestCallAction).parameters.filter { it.name.toLowerCase() == pss.last().toLowerCase() }
                                .forEach {param->
                                    dbActions.forEach { db->
                                        ParamHandler.bindParam(db, param,if(pss.size > 1) pss[pss.size - 2] else "", bindParamBasedOnDB )
                                    }
                                }
                    }
        }
    }

    private fun selectToDataRowDto(dbAction : DbAction, tableName : String) : DataRowDto{
        dbAction.seeGenes().forEach { assert((it is SqlPrimaryKeyGene || it is ImmutableDataHolderGene || it is SqlForeignKeyGene)) }
        val set = dbAction.seeGenes().filter { it is SqlPrimaryKeyGene }.map { ((it as SqlPrimaryKeyGene).gene as ImmutableDataHolderGene).value }.toSet()
        return randomness.choose(dataInDB[tableName]!!.filter { it.columnData.toSet().equals(set) })
    }

    //tables
    private fun hasDBHandler() : Boolean = sampler is RestSamplerII && (sampler as RestSamplerII).sqlInsertBuilder!= null && config.allowDataFromDB

    fun snapshotDB(){
        if(hasDBHandler()){
            (sampler as RestSamplerII).sqlInsertBuilder!!.extractExistingPKs(dataInDB)
        }
    }

    /*
        two purposes of the comparision:
        1) at the starting, check if data can be modified (if the rest follows the semantic, it should be added) by POST action of resources.
            based on the results, relationship between resource and table can be built.
        2) with the search, the relationship (resource -> table) can be further
     */
    fun compareDB(call : RestResourceCalls){

        if(hasDBHandler()){
            assert(call.doesCompareDB)

            if((sampler as RestSamplerII).sqlInsertBuilder != null){

                val previous = dataInDB.toMutableMap()
                snapshotDB()

                /*
                    using PKs, check whether any row is changed
                    TODO further check whether any value of row is changed, e.g., pk keeps same, but one value of other columns is changed (PATCH)
                 */

                val ar = call.resource.ar
                val tables = resourceTables.getOrPut(ar.path.toString()){ mutableSetOf() }
                if(isDBChanged(previous, dataInDB)){
                    tables.addAll(tableChanged(previous, dataInDB))
                    tables.addAll(tableChanged(dataInDB, previous))

                }

                if(call.dbActions.isNotEmpty()){
                    tables.addAll(call.dbActions.map { it.table.name }.toHashSet())
                }

            }
        }
    }

    private fun tableChanged(a : MutableMap<String, MutableList<DataRowDto>>,
                            b : MutableMap<String, MutableList<DataRowDto>>) : MutableSet<String>{
        val result = mutableSetOf<String>()
        a.forEach { t, u ->
            if(!b.containsKey(t)) result.add(t)
            else{
                val bcol = b[t]!!
                if(bcol.size != u.size) result.add(t)
                else {
                    val bcolContent = bcol.map { it.columnData.joinToString() }

                    loop@for(con in u){
                        if(!bcolContent.contains(con.columnData.joinToString())){
                            result.add(t)
                            break@loop
                        }
                    }
                }
            }
        }

        return result
    }

    private fun isDBChanged(previous : MutableMap<String, MutableList<DataRowDto>>,
                  current : MutableMap<String, MutableList<DataRowDto>>) : Boolean{
        if(previous.size != current.size) return true
        for(entry in current){
            val pre = previous[entry.key] ?: return true
            if(entry.value.size != pre!!.size) return true
            val preData = pre!!.map { it.columnData.joinToString() }
            for(cdata in entry.value){
                if(!preData.contains(cdata.columnData.joinToString())) return true
            }
        }
        return false
    }

    fun getResourceCluster() : Map<String, RestAResource> {
        return resourceCluster.toMap()
    }
    fun onlyIndependentResource() : Boolean {
        if (initialized)
            return resourceCluster.values.filter{ r -> !r.isIndependent() }.isEmpty()

        throw IllegalArgumentException("resource cluster is not initialized!!")
    }

    private fun repairDbActions(dbActions: MutableList<DbAction>){
        /**
         * First repair SQL Genes (i.e. SQL Timestamps)
         */
        GeneUtils.repairGenes(dbActions.flatMap { it.seeGenes() })

        /**
         * Now repair database constraints (primary keys, foreign keys, unique fields, etc.)
         */

        DbActionUtils.repairBrokenDbActionsList(dbActions, randomness)
    }

}