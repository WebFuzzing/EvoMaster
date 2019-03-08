package org.evomaster.core.problem.rest2.resources

import com.google.inject.Inject
import org.evomaster.client.java.controller.api.dto.database.operations.DataRowDto
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.core.problem.rest.RestAction
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest.SampleType
import org.evomaster.core.problem.rest.auth.AuthenticationInfo
import org.evomaster.core.problem.rest.serviceII.ParamHandler
import org.evomaster.core.problem.rest.serviceII.RestIndividualII
import org.evomaster.core.problem.rest.serviceII.RestSamplerII
import org.evomaster.core.problem.rest.serviceII.resources.RestAResource
import org.evomaster.core.problem.rest.serviceII.resources.RestResourceCalls
import org.evomaster.core.problem.rest2.resources.dependency.ParamRelatedToTable
import org.evomaster.core.problem.rest2.resources.dependency.PossibleCreationChain
import org.evomaster.core.problem.rest2.resources.token.parser.ParserUtil
import org.evomaster.core.search.Action
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
    protected lateinit var randomness: Randomness

    @Inject
    protected lateinit var config: EMConfig


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

    private val relevants : MutableList<RelevantResource> = mutableListOf()


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
                if(!u.creation.isComplete()){
                    missingCreation.put(t, mutableListOf())
                }
            }
            initialized = true
        }
    }

    //resources
    fun createSingleResourceBasedOnTemplate(auth: AuthenticationInfo, adHocInitialIndividuals : MutableList<RestIndividualII>){
        resourceCluster.values.forEach { ar->
            ar.templates.forEach { t, u ->
                if(u.size > 1){
                    val call = ar.sampleRestResourceCalls(u.template, randomness, config.maxTestSize)
                    call.actions.forEach { if(it is RestCallAction) it.auth = auth }
                    adHocInitialIndividuals.add(RestIndividualII(mutableListOf(call), SampleType.SMART_RESOURCE))
                }
            }
        }

    }

    /**
     * this function is used to initialized adhoc individuals
     */
    fun createSingleResourceOnEachEndpoint(auth: AuthenticationInfo, adHocInitialIndividuals : MutableList<RestIndividualII>){
        //GET, PATCH, DELETE
        resourceCluster.values.forEach { ar->
            ar.actions.filter { it is RestCallAction && it.verb != HttpVerb.POST && it.verb != HttpVerb.PUT }.forEach {a->
                val call = ar.sampleOneAction(a.copy() as RestAction, randomness)
                adHocInitialIndividuals.add(RestIndividualII(mutableListOf(call), SampleType.SMART_RESOURCE))
            }
        }
        //POST
        resourceCluster.values.sortedBy { it.tokens.size }.asSequence().forEach {
            it.genPostChain(randomness)?.let {call->
                call.actions.forEach {a->
                    if(a is RestCallAction) a.auth = auth
                }
                call.doesCompareDB = hasDBHandler()
                adHocInitialIndividuals.add(RestIndividualII(mutableListOf(call), SampleType.SMART_RESOURCE))
            }
        }
        //PUT
        resourceCluster.values.forEach { ar->
            ar.actions.filter { it is RestCallAction && it.verb == HttpVerb.PUT }.forEach {a->
                val call = ar.sampleOneAction(a.copy() as RestAction, randomness)
                call.actions.forEach { (it as RestCallAction).auth = auth }
                adHocInitialIndividuals.add(RestIndividualII(mutableListOf(call), SampleType.SMART_RESOURCE))
            }
        }
    }

    fun createSingleResourceOnEachEndpoint(action: RestAction, auth: AuthenticationInfo, adHocInitialIndividuals : MutableList<RestIndividualII>){

        val resource = if(action is RestCallAction){
            resourceCluster[action.path.toString()]
        }else {
            throw IllegalArgumentException("rest action is not rest call action")
        }

        resource?: throw IllegalArgumentException("resource path can not be found ")

        val call = resource.sampleOneAction(action, randomness)
        call.actions.forEach { (it as RestCallAction).auth = auth }
        adHocInitialIndividuals.add(RestIndividualII(mutableListOf(call), SampleType.SMART_RESOURCE))
    }

    fun generateCall(resourceKey: String, calls : MutableList<RestResourceCalls>, size : Int){
        val ar = resourceCluster[resourceKey]
                ?: throw IllegalArgumentException("resource path $resourceKey does not exist!")

        val call = ar.sampleRestResourceCalls(randomness, size)
        calls.add(call)
        if(hasDBHandler()){
            if(!ar.creation.isComplete()){
                call.doesCompareDB = true
                /*
                    derive possible db, and bind value according to db
                */
                val created = deriveRelatedTables(ar, call)
                if(!created){
                    //TODO MAN record the call when creation fails
                }
            }else{
                call.doesCompareDB = (!call.template.independent) && (resourceTables[ar.path.toString()] == null)
            }
        }
    }

    private fun deriveRelatedTables(ar: RestAResource){
        val post = ar.creation.actions.firstOrNull()
        val skip = if(post != null && (post as RestCallAction).path.isLastElementAParameter())  1 else 0

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

        //val inferTables = mutableMapOf<String, String>()
        missingParams.forEach { pname->
            //inferTables.put(pname, "")
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
//                if(similarity >= ParserUtil.SimilarityThreshold && dataInDB[tableName]!!.size > 0){
//                    inferTables.replace(pname, tableName)
//                    return@findP
//                }
                if(similarity >= ParserUtil.SimilarityThreshold){
                    //inferTables.replace(pname, tableName)
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

    private fun deriveRelatedTables(ar: RestAResource, call : RestResourceCalls) : Boolean{

        if(ar.paramsToTables.isEmpty())
            deriveRelatedTables(ar)

        if(ar.paramsToTables.values.find { it.probability < ParserUtil.SimilarityThreshold || it.targets.isEmpty()} == null){
            var empty = false

            snapshotDB()
            ar.paramsToTables.forEach { t, u ->
                val ps = u.additionalInfo.split(ParamHandler.separator)
                val t = ps.last()

                val tableName = u.targets.first().toString()

                if(dataInDB[tableName]!= null && dataInDB[tableName]!!.isNotEmpty()){

                    val dbAction = (sampler as RestSamplerII).sqlInsertBuilder!!.extractExistingByCols(tableName, randomness.choose(dataInDB[tableName]!!))
                    call.dbActions.add(dbAction)
                    /*
                        bind param value according to db action
                     */
                    call.actions.forEach { ac->
                        val pa = (ac as RestCallAction).parameters.find {
                            it.name.toLowerCase() == t.toLowerCase()
                        }
                        if(pa != null){
                            ParamHandler.bindParam(dbAction, pa, if(ps.size > 1) ps[ps.size - 2] else "")
                        }
                    }

                    /*
                        TODO [MAN] set mutable of these genes false ???
                     */
                    call.isDataFromBD = true

                }else
                    empty = true

            }

            return !empty
        }
        return false
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

                if(call.isDataFromBD){
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
    fun onlyIndependentResource() : Boolean? = if (initialized) resourceCluster.values.filter{ r -> !r.independent }.isEmpty() else null

}