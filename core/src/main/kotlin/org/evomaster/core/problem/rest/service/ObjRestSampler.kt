package org.evomaster.core.problem.rest.service

import com.google.inject.Inject
import io.swagger.models.Swagger
import io.swagger.parser.SwaggerParser
import org.evomaster.client.java.controller.api.dto.SutInfoDto
import org.evomaster.core.EMConfig
import org.evomaster.core.Lazy
import org.evomaster.core.database.DbAction
import org.evomaster.core.database.DbActionUtils
import org.evomaster.core.database.SqlInsertBuilder
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.problem.rest.*
import org.evomaster.core.problem.rest.auth.AuthenticationHeader
import org.evomaster.core.problem.rest.auth.AuthenticationInfo
import org.evomaster.core.problem.rest.auth.NoAuth
import org.evomaster.core.problem.rest.param.PathParam
import org.evomaster.core.remote.SutProblemException
import org.evomaster.core.remote.service.RemoteController
import org.evomaster.core.search.Action
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.service.Sampler
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.ConnectException
import javax.annotation.PostConstruct
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response


class ObjRestSampler : Sampler<RestIndividual>() {

    companion object {
        val log: Logger = LoggerFactory.getLogger(ObjRestSampler::class.java)
    }

    @Inject
    private lateinit var rc: RemoteController

    @Inject
    private lateinit var configuration: EMConfig


    private val authentications: MutableList<AuthenticationInfo> = mutableListOf()

    private val adHocInitialIndividuals: MutableList<RestAction> = mutableListOf()

    private var sqlInsertBuilder: SqlInsertBuilder? = null

    var existingSqlData : List<DbAction> = listOf()
        private set

    private val modelCluster: MutableMap<String, ObjectGene> = mutableMapOf()

    private val usedObjects: UsedObjs = UsedObjs()

    @PostConstruct
    private fun initialize() {

        log.debug("Initializing {}", ObjRestSampler::class.simpleName)

        rc.checkConnection()

        val started = rc.startSUT()
        if (!started) {
            throw SutProblemException("Failed to start the system under test")
        }

        val infoDto = rc.getSutInfo()
                ?: throw SutProblemException("Failed to retrieve the info about the system under test")

        val swagger = getSwagger(infoDto)
        if (swagger.paths == null) {
            throw SutProblemException("There is no endpoint definition in the retrieved Swagger file")
        }

        actionCluster.clear()
        ObjRestActionBuilder.addActionsFromSwagger(swagger, actionCluster, infoDto.restProblem?.endpointsToSkip ?: listOf())

        modelCluster.clear()
        ObjRestActionBuilder.getModelsFromSwagger(swagger, modelCluster)

        setupAuthentication(infoDto)

        initAdHocInitialIndividuals()

        if (infoDto.sqlSchemaDto != null && configuration.shouldGenerateSqlData()) {
            sqlInsertBuilder = SqlInsertBuilder(infoDto.sqlSchemaDto, rc)
            existingSqlData = sqlInsertBuilder!!.extractExistingPKs()
        }

        if(configuration.outputFormat == OutputFormat.DEFAULT){
            try {
                val format = OutputFormat.valueOf(infoDto.defaultOutputFormat?.toString()!!)
                configuration.outputFormat = format
            } catch (e : Exception){
                throw SutProblemException("Failed to use test output format: " + infoDto.defaultOutputFormat)
            }
        }

        log.debug("Done initializing {}", ObjRestSampler::class.simpleName)
    }

    private fun setupAuthentication(infoDto: SutInfoDto) {

        val info = infoDto.infoForAuthentication ?: return

        info.forEach { i ->
            if (i.name == null || i.name.isBlank()) {
                log.warn("Missing name in authentication info")
                return@forEach
            }

            val headers: MutableList<AuthenticationHeader> = mutableListOf()

            i.headers.forEach loop@{ h ->
                val name = h.name?.trim()
                val value = h.value?.trim()
                if (name == null || value == null) {
                    log.warn("Invalid header in ${i.name}")
                    return@loop
                }

                headers.add(AuthenticationHeader(name, value))
            }

            val auth = AuthenticationInfo(i.name.trim(), headers)

            authentications.add(auth)
        }
    }


    private fun getSwagger(infoDto: SutInfoDto): Swagger {

        val swaggerURL = infoDto?.restProblem?.swaggerJsonUrl ?: throw IllegalStateException("Missing information about the Swagger URL")

        val response = connectToSwagger(swaggerURL, 30)

        if (!response.statusInfo.family.equals(Response.Status.Family.SUCCESSFUL)) {
            throw SutProblemException("Cannot retrieve Swagger JSON data from $swaggerURL , status=${response.status}")
        }

        val json = response.readEntity(String::class.java)

        val swagger = try {
            SwaggerParser().parse(json)
        } catch (e: Exception) {
            throw SutProblemException("Failed to parse Swagger JSON data: $e")
        }

        return swagger
    }

    private fun connectToSwagger(swaggerURL: String, attempts: Int): Response {

        for (i in 0 until attempts) {
            try {
                return ClientBuilder.newClient()
                        .target(swaggerURL)
                        .request(MediaType.APPLICATION_JSON_TYPE)
                        .get()
            } catch (e: Exception) {

                if (e.cause is ConnectException) {
                    /*
                        Even if SUT is running, Swagger service might not be ready
                        yet. So let's just wait a bit, and then retry
                    */
                    Thread.sleep(1_000)
                } else {
                    throw IllegalStateException("Failed to connect to $swaggerURL: ${e.message}")
                }
            }
        }

        throw IllegalStateException("Failed to connect to $swaggerURL")
    }


    fun sampleSqlInsertion(tableName: String, columns: Set<String>): List<DbAction> {

        val actions = sqlInsertBuilder?.createSqlInsertionAction(tableName, columns)
                ?: throw IllegalStateException("No DB schema is available")

        DbActionUtils.randomizeDbActionGenes(actions, randomness)
        return actions
    }

    override fun sampleAtRandom(): RestIndividual {

        val actions = mutableListOf<RestAction>()
        val n = randomness.nextInt(1, config.maxTestSize)

        usedObjects.clearLists()
        (0 until n).forEach {
            actions.add(sampleRandomAction(0.05))
        }
        val objInd = RestIndividual(actions, SampleType.RANDOM, mutableListOf(), usedObjects.copy())
        usedObjects.clearLists()
        return objInd
    }


    private fun proposeObject(g: Gene): Pair<ObjectGene, Pair<String, String>> {
        var restrictedModels = mutableMapOf<String, ObjectGene>()
        val innerGene = when (g::class) {
            ObjectGene::class -> g
            DisruptiveGene::class -> (g as DisruptiveGene<*>).gene
            OptionalGene::class -> (g as OptionalGene).gene
            else -> g
        }
        when (innerGene::class) {
            ObjectGene::class -> {
                // If the gene is an object, select a suitable one from the Model CLuster (based on the Swagger)
                restrictedModels = modelCluster.filter{ model ->
                    (model.value as ObjectGene).fields
                            .map{ it.name }
                            .toSet().containsAll((innerGene as ObjectGene).fields.map { it.name }) }.toMutableMap()
                val ret = randomness.choose(restrictedModels)
                return Pair(ret, Pair(ret.name, "Complete_object"))
            }
            else -> {
                modelCluster.forEach { k, model ->
                    val fields = model.fields.filter { field ->
                        when (field::class) {
                            OptionalGene::class -> innerGene::class === (field as OptionalGene).gene::class
                            else -> innerGene::class === field::class
                        }
                    }
                    restrictedModels[k] = ObjectGene(model.name, fields)
                }
            }
        }

        //Having filtered the objects, build the probability map.
        val likely = likelyhoodsExtended(g.getVariableName(), restrictedModels)
                .toList()
                .sortedBy { (_, value) -> -value}
                .toMap()

        if (likely.isNotEmpty()){
            // there is at least one likely match
            val selected = pickObj((likely as MutableMap<Pair<String, String>, Float>), probabilistic = true)
            val selObject = modelCluster.get(selected.first)!!
            return Pair(selObject, selected)
        }
        else{
            // there is no likely match
            val fields = listOf(g)
            val wrapper = ObjectGene("Gene_wrapper_object", fields)
            return Pair(wrapper, Pair("", "Single_gene"))
        }
    }

    private fun randomizeActionGenes(action: Action, probabilistic: Boolean = false) {
        if(!config.enableCompleteObjects) {
            action.seeGenes().forEach { it.randomize(randomness, false) }
        }
        else {
            action.seeGenes().forEach { g ->
                /*Obtain the object proposed for mutation. Can be:
                1. Complete object - the entire object is used and needs to be mutated.
                2. Just the gene g (wrapped in an ObjectGene) - an object match could not be found, g is mutated as such.
                3. The object, plus a 2 string pair - model name, gene name. This identifies an object that needs to be mutated
                and which of its genes will be used.
                */
                val (proposed, field) = proposeObject(g)
                val innerGene = when (g::class){
                    OptionalGene::class -> (g as OptionalGene).gene
                    DisruptiveGene::class -> (g as DisruptiveGene<*>).gene
                    else -> g
                }

                when(field.second) {
                    "Single_gene" -> {
                        if (g.isMutable()) g.randomize(randomness, probabilistic)
                    }
                    "Complete_object" -> {
                        proposed.randomize(randomness, probabilistic)
                        innerGene.copyValueFrom(proposed)
                        /* when(g::class) {
                            ObjectGene::class -> g.copyValueFrom(proposed)
                            OptionalGene::class ->  (g as OptionalGene).gene.copyValueFrom(proposed)
                            DisruptiveGene::class -> (g as DisruptiveGene<*>).gene.copyValueFrom(proposed)
                            else -> g.copyValueFrom(proposed)
                        }*/

                        usedObjects.assign(Pair((action as RestCallAction), g), proposed, field)
                        usedObjects.selectbody(action, proposed)
                    }
                    else -> {
                        proposed.randomize(randomness, probabilistic)
                        val proposedGene = findSelectedGene(field)
                        innerGene.copyValueFrom(proposedGene)
                        /*when (g::class) {
                            DisruptiveGene::class -> (g as DisruptiveGene<*>).gene.copyValueFrom(proposedGene)
                            OptionalGene::class -> (g as OptionalGene).gene.copyValueFrom(proposedGene)
                            else -> g.copyValueFrom(proposedGene)
                        }*/
                        usedObjects.assign(Pair((action as RestCallAction), g), proposed, field)
                        usedObjects.selectbody(action, proposed)
                    }
                }

            }
        }

    }

    private fun findSelectedGene(selectedGene: Pair<String, String>): Gene {

        val foundGene = (modelCluster[selectedGene.first]!!).fields.filter{ field ->
            field.name == selectedGene.second
        }.first()
        when (foundGene::class) {
            OptionalGene::class -> return (foundGene as OptionalGene).gene
            DisruptiveGene::class -> return (foundGene as DisruptiveGene<*>).gene
            else -> return foundGene
        }
    }

    fun sampleRandomAction(noAuthP: Double): RestAction {
        val action = randomness.choose(actionCluster).copy() as RestAction
        randomizeActionGenes(action)

        if (action is RestCallAction) {
            action.auth = getRandomAuth(noAuthP)
        }

        return action
    }

    private fun sampleRandomCallAction(noAuthP: Double): RestCallAction {
        val action = randomness.choose(actionCluster.filter { a -> a.value is RestCallAction }).copy() as RestCallAction
        randomizeActionGenes(action)
        action.auth = getRandomAuth(noAuthP)

        return action
    }


    private fun getRandomAuth(noAuthP: Double): AuthenticationInfo {
        if (authentications.isEmpty() || randomness.nextBoolean(noAuthP)) {
            return NoAuth()
        } else {
            //if there is auth, should have high probability of using one,
            //as without auth we would do little.
            return randomness.choose(authentications)
        }
    }

    override fun smartSample(): RestIndividual {

        /*
            At the beginning, sample from this set, until it is empty
         */
        if (!adHocInitialIndividuals.isEmpty()) {
            val action = adHocInitialIndividuals.removeAt(adHocInitialIndividuals.size - 1)
            usedObjects.clearLists()
            randomizeActionGenes(action, false)
            val objInd = RestIndividual(mutableListOf(action), SampleType.SMART, mutableListOf(), usedObjects.copy())
            usedObjects.clearLists()
            return objInd
        }

        if (config.maxTestSize <= 1) {
            /*
                Here we would have sequences of endpoint calls that are
                somehow linked to each other, eg a DELETE on a resource
                created with a POST.
                If can have only one call, then just go random
             */
            return sampleAtRandom()
        }


        val test = mutableListOf<RestAction>()
        usedObjects.clearLists()

        val action = sampleRandomCallAction(0.0)

        /*
            TODO: each of these "smart" tests could end with a GET, to make
            the test easier to read and verify the results (eg side-effects of
            DELETE/PUT/PATCH operations).
            But doing that as part of the tests could be inefficient (ie a lot
            of GET calls).
            Maybe that should be done as part of an "assertion generation" phase
            (which would also be useful for creating checks on returned JSONs)
         */

        val sampleType = when (action.verb) {
            HttpVerb.GET -> handleSmartGet(action, test)
            HttpVerb.POST -> handleSmartPost(action, test)
            HttpVerb.PUT -> handleSmartPut(action, test)
            HttpVerb.DELETE -> handleSmartDelete(action, test)
            HttpVerb.PATCH -> handleSmartPatch(action, test)
            else -> SampleType.RANDOM
        }

        if (!test.isEmpty()) {
            val objInd = RestIndividual(test, sampleType, mutableListOf(), usedObjects.copy())
            usedObjects.clearLists()
            return objInd
        }
        usedObjects.clearLists()
        return sampleAtRandom()
    }

    override fun hasSpecialInit(): Boolean {
        return !adHocInitialIndividuals.isEmpty() && config.probOfSmartSampling > 0
    }

    override fun resetSpecialInit() {
        initAdHocInitialIndividuals()
    }

    private fun handleSmartPost(post: RestCallAction, test: MutableList<RestAction>): SampleType {

        Lazy.assert{post.verb == HttpVerb.POST}

        //as POST is used in all the others, maybe here we do not really need to handle it specially?
        test.add(post)
        return SampleType.SMART
    }

    private fun handleSmartDelete(delete: RestCallAction, test: MutableList<RestAction>): SampleType {

        Lazy.assert{delete.verb == HttpVerb.DELETE}

        createWriteOperationAfterAPost(delete, test)

        return SampleType.SMART
    }

    private fun handleSmartPatch(patch: RestCallAction, test: MutableList<RestAction>): SampleType {

        Lazy.assert{patch.verb == HttpVerb.PATCH}

        createWriteOperationAfterAPost(patch, test)

        return SampleType.SMART
    }

    private fun handleSmartPut(put: RestCallAction, test: MutableList<RestAction>): SampleType {

        Lazy.assert{put.verb == HttpVerb.PUT}

        /*
            A PUT might be used to update an existing resource, or to create a new one
         */
        if (randomness.nextBoolean(0.2)) {
            /*
                with low prob., let's just try the PUT on its own.
                Recall we already add single calls on each endpoint at initialization
             */
            test.add(put)
            return SampleType.SMART
        }

        createWriteOperationAfterAPost(put, test)
        return SampleType.SMART
    }

    /**
     *    Only for PUT, DELETE, PATCH
     */
    private fun createWriteOperationAfterAPost(write: RestCallAction, test: MutableList<RestAction>) {

        Lazy.assert{write.verb == HttpVerb.PUT || write.verb == HttpVerb.DELETE || write.verb == HttpVerb.PATCH}

        test.add(write)

        //Need to find a POST on a parent collection resource
        createResourcesFor(write, test)

        if (write.verb == HttpVerb.PATCH &&
                config.maxTestSize >= test.size + 1 &&
                randomness.nextBoolean()) {
            /*
                As PATCH is not idempotent (in contrast to PUT), it can make sense to test
                two patches in sequence
             */
            val secondPatch = createActionFor(write, write)
            test.add(secondPatch)
            secondPatch.locationId = write.locationId
        }

        test.forEach { t ->
            preventPathParamMutation(t as RestCallAction)
        }
    }

    private fun handleSmartGet(get: RestCallAction, test: MutableList<RestAction>): SampleType {

        Lazy.assert{get.verb == HttpVerb.GET}

        /*
           A typical case is something like

           POST /elements
           GET  /elements/{id}

           Problems is that the {id} might not be known beforehand,
           eg it would be the result of calling POST first, where the
           path would be in the returned Location header.

           However, we might even encounter cases like:

           POST /elements/{id}
           GET  /elements/{id}

           which is possible, although bit weird, as in such case it
           would be better to have a PUT instead of a POST.

           Note: we prefer a POST to create a resource, as that is the
           most common case, and not all PUTs allow creation
         */

        test.add(get)

        val created = createResourcesFor(get, test)

        if (!created) {
            /*
                A GET with no POST in any ancestor.
                This could happen if the API is "read-only".

                TODO: In such case, would really need to handle things like
                direct creation of data in the DB (for example)
             */
        } else {
            //only lock path params if it is not a single GET
            test.forEach { t ->
                preventPathParamMutation(t as RestCallAction)
            }
        }

        if (created && !get.path.isLastElementAParameter()) {

            val lastPost = test[test.size - 2] as RestCallAction
            Lazy.assert{lastPost.verb == HttpVerb.POST}

            val available = config.maxTestSize - test.size

            if (lastPost.path.isEquivalent(get.path) && available > 0) {
                /*
                 The endpoint might represent a collection, ie we
                 can be in the case:

                  POST /api/elements
                  GET  /api/elements

                 Therefore, to properly test the GET, we might
                 need to be able to create many elements.
                 */
                val k = 1 + randomness.nextInt(available)

                (0 until k).forEach {
                    val create = createActionFor(lastPost, get)
                    preventPathParamMutation(create)
                    create.locationId = lastPost.locationId

                    //add just before the last GET
                    test.add(test.size - 1, create)
                }

                return SampleType.SMART_GET_COLLECTION
            }
        }

        return SampleType.SMART
    }


    private fun createResourcesFor(target: RestCallAction, test: MutableList<RestAction>)
            : Boolean {

        if (test.size >= config.maxTestSize) {
            return false
        }

        val template = chooseClosestAncestor(target, listOf(HttpVerb.POST))
                ?: return false

        val post = createActionFor(template, target)

        test.add(0, post)

        /*
            Check if POST depends itself on the creation of
            some intermediate resource
         */
        if (post.path.hasVariablePathParameters() &&
                (!post.path.isLastElementAParameter()) ||
                post.path.getVariableNames().size >= 2) {

            val dependencyCreated = createResourcesFor(post, test)
            if (!dependencyCreated) {
                return false
            }
        }


        /*
            Once the POST is fully initialized, need to fix
            links with target
         */
        if (!post.path.isEquivalent(target.path)) {
            /*
                eg
                POST /x
                GET  /x/{id}
             */
            post.saveLocation = true
            target.locationId = post.path.lastElement()
        } else {
            /*
                eg
                POST /x
                POST /x/{id}/y
                GET  /x/{id}/y
             */
            //not going to save the position of last POST, as same as target
            post.saveLocation = false

            // the target (eg GET) needs to use the location of first POST, or more correctly
            // the same location used for the last POST (in case there is a deeper chain)
            target.locationId = post.locationId
        }

        return true
    }

    private fun preventPathParamMutation(action: RestCallAction) {
        action.parameters.forEach { p -> if (p is PathParam) p.preventMutation() }
    }

    fun createActionFor(template: RestCallAction, target: RestCallAction): RestCallAction {
        val res = template.copy() as RestCallAction
        randomizeActionGenes(res)
        res.auth = target.auth
        res.bindToSamePathResolution(target)
        //TODO this is a candidate for a place to synch objects with paths between related actions?

        return res
    }

    /**
     * Make sure that what returned is different from the target.
     * This can be a strict ancestor (shorter path), or same
     * endpoint but with different HTTP verb.
     * Among the different ancestors, return one of the longest
     */
    private fun chooseClosestAncestor(target: RestCallAction, verbs: List<HttpVerb>): RestCallAction? {

        var others = sameOrAncestorEndpoints(target.path)
        others = hasWithVerbs(others, verbs).filter { t -> t.getName() != target.getName() }

        if (others.isEmpty()) {
            return null
        }

        return chooseLongestPath(others)
    }

    /**
     * Get all ancestor (same path prefix) endpoints that do at least one
     * of the specified operations
     */
    private fun sameOrAncestorEndpoints(path: RestPath): List<RestCallAction> {
        return actionCluster.values.asSequence()
                .filter { a -> a is RestCallAction && a.path.isAncestorOf(path) }
                .map { a -> a as RestCallAction }
                .toList()
    }

    private fun chooseLongestPath(actions: List<RestCallAction>): RestCallAction {

        if (actions.isEmpty()) {
            throw IllegalArgumentException("Cannot choose from an empty collection")
        }

        val max = actions.asSequence().map { a -> a.path.levels() }.max()!!
        val candidates = actions.filter { a -> a.path.levels() == max }

        return randomness.choose(candidates)
    }

    private fun hasWithVerbs(actions: List<RestCallAction>, verbs: List<HttpVerb>): List<RestCallAction> {
        return actions.filter { a ->
            verbs.contains(a.verb)
        }
    }


    private fun initAdHocInitialIndividuals() {

        adHocInitialIndividuals.clear()

        //init first sampling with 1-action call per endpoint, for all auths

        createSingleCallOnEachEndpoint(NoAuth())

        authentications.forEach { auth ->
            createSingleCallOnEachEndpoint(auth)
        }
    }

    private fun createSingleCallOnEachEndpoint(auth: AuthenticationInfo) {
        actionCluster.asSequence()
                .filter { a -> a.value is RestCallAction }
                .forEach { a ->
                    val copy = a.value.copy() as RestCallAction
                    copy.auth = auth
                    randomizeActionGenes(copy)
                    adHocInitialIndividuals.add(copy)
                }
    }



    fun lcs(a: String, b: String): String {
        if (a.length > b.length) return lcs(b, a)
        var res = ""
        for (ai in 0 until a.length) {
            for (len in a.length - ai downTo 1) {
                for (bi in 0 until b.length - len + 1) {
                    if (a.regionMatches(ai, b, bi,len) && len > res.length) {
                        res = a.substring(ai, ai + len)
                    }
                }
            }
        }
        return res
    }

    fun pickWithProbability(map: MutableMap<Pair<String, String>, Float>): Pair<String, String>{
        val randFl = randomness.nextFloat()
        var temp = 0.toFloat()
        var found = map.keys.first()

        for((k, v) in map){
            if(randFl <= (v + temp)){
                found = k
                break
            }
            temp += v
        }
        return found
    }




    fun <T> likelyhoodsExtended(parameter: String, candidates: MutableMap<String, T>): MutableMap<Pair<String, String>, Float>{
        //TODO BMR: account for empty candidate sets.
        val result = mutableMapOf<Pair<String, String>, Float>()
        var sum : Float = 0.toFloat()

        candidates.forEach { k, v ->
            for (field in (v as ObjectGene).fields) {
                val fieldName = field.name
                val extendedName = "$k${fieldName}"
                val temp = 1.toFloat() + lcs(parameter.toLowerCase(), extendedName.toLowerCase()).length.toFloat()/ Integer.max(parameter.length, extendedName.length).toFloat()
                result[Pair(k, fieldName)] = temp
                sum += temp
            }
        }
        result.forEach { k, u ->
            result[k] = u/sum
        }

        return result
    }

    fun pickObj(map: MutableMap<Pair<String, String>, Float>, probabilistic: Boolean = true ): Pair<String, String>{

        var found = map.keys.first()
        if (probabilistic) {
            found = pickWithProbability(map)
        }
        return found
    }
}