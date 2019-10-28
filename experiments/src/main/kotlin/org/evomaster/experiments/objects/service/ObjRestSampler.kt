package org.evomaster.experiments.objects.service

import com.google.inject.Inject
import io.swagger.models.Swagger
import io.swagger.parser.SwaggerParser
import org.evomaster.client.java.controller.api.dto.SutInfoDto
import org.evomaster.core.EMConfig
import org.evomaster.core.database.DbAction
import org.evomaster.core.database.DbActionUtils
import org.evomaster.core.database.SqlInsertBuilder
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.problem.rest.*
import org.evomaster.core.problem.rest.auth.AuthenticationHeader
import org.evomaster.core.problem.rest.auth.AuthenticationInfo
import org.evomaster.core.problem.rest.auth.NoAuth
import org.evomaster.core.remote.SutProblemException
import org.evomaster.core.remote.service.RemoteController
import org.evomaster.core.search.Action
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.service.Sampler
import org.evomaster.experiments.objects.*
import org.evomaster.experiments.objects.ObjIndividual
import org.evomaster.experiments.objects.ObjRestActionBuilder
import org.evomaster.experiments.objects.RestPath
import org.evomaster.experiments.objects.UsedObj
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.ConnectException
import javax.annotation.PostConstruct
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import org.evomaster.experiments.objects.param.PathParam


class ObjRestSampler : Sampler<ObjIndividual>() {

    companion object {
        val log: Logger = LoggerFactory.getLogger(ObjRestSampler::class.java)
    }

    @Inject
    private lateinit var rc: RemoteController

    @Inject
    private lateinit var configuration: EMConfig


    private val authentications: MutableList<AuthenticationInfo> = mutableListOf()

    private val adHocInitialIndividuals: MutableList<ObjRestCallAction> = mutableListOf()

    private var sqlInsertBuilder: SqlInsertBuilder? = null

    var existingSqlData : List<DbAction> = listOf()
        private set

    private val modelCluster: MutableMap<String, ObjectGene> = mutableMapOf()

    private val usedObject: UsedObj = UsedObj()
    //private val usedObj: MutableMap<Pair<String, String> , ObjectGene> = mutableMapOf()
    //private val usedObj: MutableMap<Pair<ObjRestCallAction, String> , ObjectGene> = mutableMapOf()

    //private val usedObj: MutableList<ObjectGene> = mutableListOf()

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
        ObjRestActionBuilder.addActionsFromSwagger(swagger, actionCluster, infoDto.restProblem?.endpointsToSkip
                ?: listOf())

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

        ObjRestSampler.log.debug("Done initializing {}", ObjRestSampler::class.simpleName)
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

            val auth = AuthenticationInfo(i.name.trim(), headers, null)

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

    override fun sampleAtRandom(): ObjIndividual {

        val actions = mutableListOf<RestAction>()
        val n = randomness.nextInt(1, config.maxTestSize)

        usedObject.clearLists()
        (0 until n).forEach {
            //actions.add(sampleRandomAction(0.05))
            actions.add(sampleRandomObjCallAction(0.05))
        }
        usedObject.coherenceCheck()
        //usedObjects.pruneObjects(actions)
        val objInd = ObjIndividual(actions, SampleType.RANDOM, usedObject.copy())
        usedObject.clearLists()
        objInd.checkCoherence()
        debuggingOnly(objInd)
        return objInd
    }


    private fun randomizeActionGenes(action: Action, probabilistic: Boolean = true) {
        action.seeGenes().forEach {g ->

            val restrictedModels = mutableMapOf<String, ObjectGene>()

            /** This part is meant to select objects and fields likely to match the gene in question
             * BUT: if the gene in question is itself an object, one item is selected from
             * the modelCluster, rather than the object-field pair
             */
            modelCluster.forEach{ k, model ->
                val fields = model.fields.filter { field ->
                    when (g::class) {
                        DisruptiveGene::class -> (field as OptionalGene).gene::class === (g as DisruptiveGene<*>).gene::class
                        OptionalGene::class -> (field as OptionalGene).gene::class === (g as OptionalGene).gene::class
                        ObjectGene::class -> (field as OptionalGene).gene::class === ObjectGene::class
                        else -> {
                            false
                        }
                    }
                }
                restrictedModels[k] = ObjectGene(model.name, fields)
            }


            /** Selecting a likely model and a likely field to use for a parameter */
            val likely = likelyhoodsExtended(g.getVariableName(), restrictedModels)
                    .toList()
                    .sortedBy { (_, value) -> -value}
                    .toMap()

            if (likely.size > 0){
                val (temp, sel) = selectGene(g, probabilistic)

                temp.gene.randomize(randomness, true)
                val copyvalue = (temp.gene as ObjectGene).fields.filter{f -> f.getVariableName() === sel.second}.first()
                try{
                    when (g::class) {
                        DisruptiveGene::class -> (g as DisruptiveGene<*>).gene.copyValueFrom(
                                (copyvalue as OptionalGene).gene
                            )
                        OptionalGene::class ->  (g as OptionalGene).gene.copyValueFrom(
                                (copyvalue as OptionalGene).gene
                        )
                        ObjectGene::class -> (g as ObjectGene).copyValueFrom(temp.gene)
                    }
                    usedObject.assign(Pair((action as ObjRestCallAction), g), temp.gene, sel)
                    //usedObjects.assign(Pair((action as ObjRestCallAction).resolvedPath(), g.getVariableName()), temp.gene, sel)
                    usedObject.selectbody((action as ObjRestCallAction), temp.gene)
                }
                catch(e: Exception){
                    g.randomize(randomness, true)
                }
            }
            else{
                /**BMR: This means that, for some reason, smart objects could not be created.
                 * This could be because:
                 * 1. the API only uses basic types
                 * 2. There is a mismatch between the required types (by the Call Action) and the found types in
                 * available objects
                 *
                 * To avoid excessive restrictions being placed on the search, in this case, just randomize without objects.
                 * */
                g.randomize(randomness, true)
            }

        }
        usedObject.coherenceCheck()
    }

    fun selectGene(g: Gene, probabilistic: Boolean = true): Pair<OptionalGene, Pair<String, String>>{
        when (g::class) {
            ObjectGene::class -> return selectObjectGene(g as ObjectGene, probabilistic)
            DisruptiveGene::class -> return selectInnerGene((g as DisruptiveGene<*>).gene, probabilistic)
            OptionalGene::class -> return selectInnerGene((g as OptionalGene).gene, probabilistic)
            else -> {
                return selectRegularGene(g)
            }
        }
    }

    fun selectInnerGene(innerGene: Gene, probabilistic: Boolean = true): Pair<OptionalGene, Pair<String, String>>{
        when(innerGene::class){
            ObjectGene::class -> return selectObjectGene(innerGene as ObjectGene, probabilistic)
            ArrayGene::class -> return selectObjectGene(innerGene as ObjectGene, probabilistic)
            else -> return selectRegularGene(innerGene, probabilistic)
        }
    }

    fun selectRegularGene(g: Gene, probabilistic: Boolean = true): Pair<OptionalGene, Pair<String, String>>{
        val restrictedModels = mutableMapOf<String, ObjectGene>()
        modelCluster.forEach{ k, model ->
            val fields = model.fields.filter { field ->
                when (g::class) {
                    DisruptiveGene::class -> (field as OptionalGene).gene::class === g::class
                    OptionalGene::class -> (field as OptionalGene).gene::class === g::class
                    else -> (field as OptionalGene).gene::class === g::class

                }
            }
            restrictedModels[k] = ObjectGene(model.name, fields)
        }

        /** Selecting a likely model and a likely field to use for a parameter */
        val likely = likelyhoodsExtended(g.getVariableName(), restrictedModels).toList().sortedBy { (_, value) -> -value}.toMap()
        val sel = pickObj(likely as MutableMap<Pair<String, String>, Float>, probabilistic)
        val temp = modelCluster.get(sel.first) as ObjectGene

        temp.randomize(randomness, true)

        val selectedGene = temp.fields.filter { g -> g.name === sel.second }?.single() as OptionalGene

        g.copyValueFrom(selectedGene.gene)

        return Pair(OptionalGene(sel.first, temp), sel)
    }

    fun selectObjectGene(g: ObjectGene, probabilistic: Boolean = true): Pair<OptionalGene, Pair<String, String>>{
        val restrictedModels = modelCluster.filter { (k, m) -> (m as ObjectGene).fields.size == (g as ObjectGene).fields.size }
        //TODO BMR: field size does not appear to be enough to avoid problems
        val selected = restrictedModels.keys.random()
        val selectedGene = OptionalGene("Object", restrictedModels[selected]!!)
        try{
            g.copyValueFrom(selectedGene.gene as ObjectGene)
        }
        catch(e: java.lang.Exception){
            g.randomize(randomness, true)
        }
        return Pair((selectedGene as OptionalGene), Pair(selected, ""))
    }

    fun sampleRandomAction(noAuthP: Double): RestAction {
        val action = randomness.choose(actionCluster).copy() as RestAction
        randomizeActionGenes(action)

        if (action is ObjRestCallAction) {
            action.auth = getRandomAuth(noAuthP)
        }

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

    override fun smartSample(): ObjIndividual {

        /*
            At the beginning, sample from this set, until it is empty
         */

        if (!adHocInitialIndividuals.isEmpty()) {
            val action = adHocInitialIndividuals.removeAt(adHocInitialIndividuals.size - 1)
            //BMR: trying to fix the initial usedObjects problem
            usedObject.clearLists()
            randomizeActionGenes(action, false)
            return ObjIndividual(mutableListOf(action), SampleType.SMART, usedObject)
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
        usedObject.clearLists()

        var action = sampleRandomObjCallAction(0.0)

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
            HttpVerb.POST -> {
                handleSmartPost(action, test)
            }
            HttpVerb.PUT -> handleSmartPut(action, test)
            HttpVerb.DELETE -> handleSmartDelete(action, test)
            HttpVerb.PATCH -> handleSmartPatch(action, test)
            else -> SampleType.RANDOM
        }
        usedObject.coherenceCheck()
        //usedObjects.pruneObjects(test)
        if (!test.isEmpty()) {
            val objInd = ObjIndividual(test, sampleType, usedObject.copy())
            usedObject.clearLists()
            objInd.checkCoherence()
            debuggingOnly(objInd)
            return objInd
        }
        usedObject.clearLists()
        return sampleAtRandom()
    }

    override fun hasSpecialInit(): Boolean {
        return !adHocInitialIndividuals.isEmpty() && config.probOfSmartSampling > 0
    }

    override fun resetSpecialInit() {
        initAdHocInitialIndividuals()
    }

    private fun handleSmartPost(post: ObjRestCallAction, test: MutableList<RestAction>): SampleType {

        assert(post.verb == HttpVerb.POST)
        //as POST is used in all the others, maybe here we do not really need to handle it specially?
        /*BMR: we may need to handle adding full objects (e.g. to the body of the call)
            e.g. sending an complex object to the server, and then retrieving some of its member objects
        */
        test.add(post)
        return SampleType.SMART
    }

    private fun handleSmartDelete(delete: ObjRestCallAction, test: MutableList<RestAction>): SampleType {

        assert(delete.verb == HttpVerb.DELETE)

        createWriteOperationAfterAPost(delete, test)

        return SampleType.SMART
    }

    private fun handleSmartPatch(patch: ObjRestCallAction, test: MutableList<RestAction>): SampleType {

        assert(patch.verb == HttpVerb.PATCH)

        createWriteOperationAfterAPost(patch, test)

        return SampleType.SMART
    }

    private fun handleSmartPut(put: ObjRestCallAction, test: MutableList<RestAction>): SampleType {

        assert(put.verb == HttpVerb.PUT)

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
    private fun createWriteOperationAfterAPost(write: ObjRestCallAction, test: MutableList<RestAction>) {

        assert(write.verb == HttpVerb.PUT || write.verb == HttpVerb.DELETE || write.verb == HttpVerb.PATCH)

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
            preventPathParamMutation(t as ObjRestCallAction)
        }
    }

    private fun handleSmartGet(get: ObjRestCallAction, test: MutableList<RestAction>): SampleType {

        assert(get.verb == HttpVerb.GET)

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
                preventPathParamMutation(t as ObjRestCallAction)
            }
        }

        if (created && !get.path.isLastElementAParameter()) {

            val lastPost = test[test.size - 2] as ObjRestCallAction
            assert(lastPost.verb == HttpVerb.POST)

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


    private fun createResourcesFor(target: ObjRestCallAction, test: MutableList<RestAction>)
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

    private fun preventPathParamMutation(callAction: ObjRestCallAction) {
        callAction.parameters.forEach { p -> if (p is PathParam) p.preventMutation() }
    }

    fun createActionFor(template: ObjRestCallAction, target: ObjRestCallAction): ObjRestCallAction {
        val res = template.copy() as ObjRestCallAction
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
    private fun chooseClosestAncestor(target: ObjRestCallAction, verbs: List<HttpVerb>): ObjRestCallAction? {

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
    private fun sameOrAncestorEndpoints(path: RestPath): List<ObjRestCallAction> {
        return actionCluster.values.asSequence()
                .filter { a -> a is ObjRestCallAction && a.path.isAncestorOf(path) }
                .map { a -> a as ObjRestCallAction }
                .toList()
    }

    private fun chooseLongestPath(callActions: List<ObjRestCallAction>): ObjRestCallAction {

        if (callActions.isEmpty()) {
            throw IllegalArgumentException("Cannot choose from an empty collection")
        }

        val max = callActions.asSequence().map { a -> a.path.levels() }.max()!!
        val candidates = callActions.filter { a -> a.path.levels() == max }

        return randomness.choose(candidates)
    }

    private fun hasWithVerbs(callActions: List<ObjRestCallAction>, verbs: List<HttpVerb>): List<ObjRestCallAction> {
        return callActions.filter { a ->
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
                .filter { a -> a.value is ObjRestCallAction }
                .forEach { a ->
                    val copy = a.value.copy() as ObjRestCallAction
                    copy.auth = auth
                    randomizeActionGenes(copy)
                    adHocInitialIndividuals.add(copy)
                }
    }

    fun getModelCluster() :MutableMap<String, ObjectGene>{
        //TODO remove this horrible hack
        return modelCluster
    }

    fun sampleRandomObjCallAction(noAuthP: Double): ObjRestCallAction {
        val action = randomness.choose(actionCluster.filter { a -> a.value is ObjRestCallAction }).copy() as ObjRestCallAction
        randomizeActionGenes(action)
        action.auth = getRandomAuth(noAuthP)

        return action
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

        val result = mutableMapOf<Pair<String, String>, Float>()
        var sum : Float = 0.toFloat()

        candidates.forEach { k, v ->
            for (field in (v as ObjectGene).fields) {
                val fieldName = field.name

                val extendedName = "$k${fieldName}"
                val temp = lcs(parameter.toLowerCase(), extendedName.toLowerCase()).length.toFloat()/ Integer.max(parameter.length, extendedName.length).toFloat()
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


    fun debuggingOnly(individual: ObjIndividual){
        println(" -- Sampler has produced -- ")
        println("${individual.debugginPrint()} => ${individual.debugginPrintProcessed()} = Uses => ${individual.usedObject.mapping.values.map { it.getValueAsPrintableString(targetFormat = null) }}")
        println("Valid? Well... ${individual.checkCoherence()}")
    }
}