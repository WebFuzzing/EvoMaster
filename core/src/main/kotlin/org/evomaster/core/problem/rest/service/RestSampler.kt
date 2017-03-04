package org.evomaster.core.problem.rest.service

import com.google.inject.Inject
import io.swagger.models.Swagger
import io.swagger.parser.SwaggerParser
import org.evomaster.clientJava.controllerApi.dto.SutInfoDto
import org.evomaster.core.problem.rest.*
import org.evomaster.core.problem.rest.auth.AuthenticationHeader
import org.evomaster.core.problem.rest.auth.AuthenticationInfo
import org.evomaster.core.problem.rest.auth.NoAuth
import org.evomaster.core.problem.rest.param.PathParam
import org.evomaster.core.search.Action
import org.evomaster.core.search.service.Sampler
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.annotation.PostConstruct
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response


class RestSampler : Sampler<RestIndividual>() {

    companion object {
        val log: Logger = LoggerFactory.getLogger(RestSampler::class.java)
    }

    @Inject
    private lateinit var rc: RemoteController

    private val authentications: MutableList<AuthenticationInfo> = mutableListOf()

    private val adHocInitialIndividuals: MutableList<RestAction> = mutableListOf()


    @PostConstruct
    private fun initialize() {

        val started = rc.startSUT()
        if (!started) {
            throw IllegalStateException("Cannot communicate with remote REST controller")
        }

        val infoDto = rc.getSutInfo() ?: throw IllegalStateException("Cannot retrieve SUT info")

        val swagger = getSwagger(infoDto)
        if (swagger.paths == null) {
            log.warn("There is no endpoint definition in the retrieved Swagger file")
        }

        RestActionBuilder().createActions(swagger, actionCluster)

        setupAuthentication(infoDto)

        initAdHocInitialIndividuals()
    }


    private fun setupAuthentication(infoDto: SutInfoDto) {

        val info = infoDto.infoForAuthentication ?: return

        info.forEach { i ->
            if (i.name == null || i.name.isBlank()) {
                log.warn("Missing name in authentication info")
                return@forEach
            }

            val headers: MutableList<AuthenticationHeader> = mutableListOf()

            i.headers.forEach loop@ { h ->
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

        val swaggerURL = infoDto.swaggerJsonUrl ?:
                throw IllegalStateException("Cannot retrieve Swagger URL")

        val response = try {
            ClientBuilder.newClient()
                    .target(swaggerURL)
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .get()
        } catch (e: Exception) {
            throw IllegalStateException("Failed to connect to $swaggerURL: ${e.message}")
        }

        if (!response.statusInfo.family.equals(Response.Status.Family.SUCCESSFUL)) {
            throw IllegalStateException("Cannot retrieve Swagger JSON data from $swaggerURL , status=${response.status}")
        }

        val json = response.readEntity(String::class.java)

        val swagger = try {
            SwaggerParser().parse(json)
        } catch (e: Exception) {
            throw IllegalStateException("Failed to parse Swagger JSON data", e)
        }

        return swagger
    }


    override fun sampleAtRandom(): RestIndividual {

        val actions = mutableListOf<RestAction>()
        val n = randomness.nextInt(1, config.maxTestSize)

        (0 until n).forEach {
            actions.add(sampleRandomAction(0.05))
        }

        return RestIndividual(actions, SampleType.RANDOM)
    }


    private fun randomizeActionGenes(action: Action) {
        action.seeGenes().forEach { g -> g.randomize(randomness, false) }
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
            return RestIndividual(mutableListOf(action), SampleType.SMART)
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

        val action = sampleRandomCallAction(0.0)

        /*
            TODO: so far, we only handle endpoints with a single path
            parameter. would need to handle the cases in which we need
            several POSTs on different ancestor resources.

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
            return RestIndividual(test, sampleType)
        }

        return sampleAtRandom()
    }

    private fun handleSmartPost(post: RestCallAction, test: MutableList<RestAction>): SampleType {

        assert(post.verb == HttpVerb.POST)

        //as POST is used in all the others, maybe here we do not really need to handle it specially?
        test.add(post)
        return SampleType.SMART
    }

    private fun handleSmartDelete(delete: RestCallAction, test: MutableList<RestAction>): SampleType {

        assert(delete.verb == HttpVerb.DELETE)

        createWriteOperationAfterAPost(delete, test)

        return SampleType.SMART
    }

    private fun handleSmartPatch(patch: RestCallAction, test: MutableList<RestAction>): SampleType {

        assert(patch.verb == HttpVerb.PATCH)

        createWriteOperationAfterAPost(patch, test)

        return SampleType.SMART
    }

    private fun handleSmartPut(put: RestCallAction, test: MutableList<RestAction>): SampleType {

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
    Only for PUT, DELETE, PATCH
     */
    private fun createWriteOperationAfterAPost(write: RestCallAction, test: MutableList<RestAction>) {

        assert(write.verb == HttpVerb.PUT || write.verb == HttpVerb.DELETE || write.verb == HttpVerb.PATCH)

        val others = sameEndpoints(write.path)
        val postOnSamePath = hasWithVerbs(others, listOf(HttpVerb.POST))

        if (!postOnSamePath.isEmpty()) {
            /*
                possible to do a direct POST on the resource.
                bit weird (ie, a PUT would make more sense), but possible
             */

            val template = postOnSamePath.first()
            val post = createActionFor(template, write)

            test.add(post)
            test.add(write)

            /*
                make sure the paths are not mutated, otherwise the write might be
                on different resource compared to POST
             */
            preventPathParamMutation(post)
            preventPathParamMutation(write)

            if (write.verb == HttpVerb.PATCH && config.maxTestSize >= 3 && randomness.nextBoolean()) {
                /*
                    As PATCH is not idempotent (in contrast to PUT), it can make sense to test
                    two patches in sequence
                 */
                val secondPatch = createActionFor(write, write)
                preventPathParamMutation(secondPatch)
                test.add(secondPatch)
            }

            return

        } else {

            test.add(write)

            //Need to find a POST on a parent collection resource
            var post = createResourceFor(write, test)

            while(post != null && post.path.hasVariablePathParameters()){
                post = createResourceFor(post, test)
            }

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
                (t as RestCallAction)
                        .let { preventPathParamMutation(it) }
            }

            return
        }
    }

    private fun handleSmartGet(get: RestCallAction, test: MutableList<RestAction>): SampleType {

        assert(get.verb == HttpVerb.GET)

        // get on a single resource, or a collection?
        val others = sameEndpoints(get.path)

        val createActions = hasWithVerbs(others, listOf(HttpVerb.POST, HttpVerb.PUT))
        if (!createActions.isEmpty()) {
            //can try to create elements

            val chosen = randomness.choose(createActions)

            when (chosen.verb) {
                HttpVerb.POST -> {
                    if (!get.path.isLastElementAParameter()) {
                        /*
                            The endpoint might represent a collection.
                            Therefore, to properly test the GET, we might
                            need to be able to create many elements.

                            TODO but what if there are other params in the path?
                            eg, a collection inside another element. need to handle
                            it as well
                         */
                        val k = 1 + randomness.nextInt(config.maxTestSize - 1)

                        (0..k).forEach {
                            val create = createActionFor(chosen, get)
                            preventPathParamMutation(create)
                            test.add(create)
                        }
                        preventPathParamMutation(get)
                        test.add(get)
                        return SampleType.SMART_GET_COLLECTION

                    } else {
                        /*
                           A POST on a ../{var} is weird, as one would rather expect
                           a PUT there. However, could still be feasible
                         */
                        val create = createActionFor(chosen, get)
                        test.add(create)
                        test.add(get)
                        preventPathParamMutation(create)
                        preventPathParamMutation(get)

                        return SampleType.SMART
                    }
                }
                HttpVerb.PUT -> {
                    val create = createActionFor(chosen, get)
                    test.add(create)
                    test.add(get)
                    preventPathParamMutation(create)
                    preventPathParamMutation(get)

                    return SampleType.SMART
                }
            }
        } else {
            /*
               Cannot create directly. Check if other endpoints might.
               A typical case is something like

               POST /elements
               GET  /elements/{id}

               Problems is that the {id} might not be known beforehand,
               eg it would be the result of calling POST first, where the
               path would be in the returned Location header.
             */

            test.add(get)

            var post = createResourceFor(get, test)

            if(post == null){
                /*
                    A GET with no POST in any ancestor.
                    This could happen if the API is "read-only".

                    TODO: In such case, would really need to handle things like
                    direct creation of data in the DB (for example)
                 */
            }

            while(post != null && post.path.hasVariablePathParameters()){
                post = createResourceFor(post, test)
                if(post == null){
                    //TODO as above, would need direct SQL, although this
                    //case should really be rare
                }
            }

            test.forEach { t ->
                (t as RestCallAction)
                        .let { preventPathParamMutation(it) }
            }

            return SampleType.SMART
        }

        return SampleType.SMART
    }


    private fun createResourceFor(target: RestCallAction, test: MutableList<RestAction>)
        : RestCallAction?{

        if(test.size >= config.maxTestSize){
            return null
        }

        val template = chooseClosestAncestor(target, listOf(HttpVerb.POST))
            ?: return null

        val post = createActionFor(template, target)
        post.saveLocation = true
        target.locationId = post.path.lastElement()
        test.add(0, post)

        return post
    }

    private fun preventPathParamMutation(action: RestCallAction) {
        action.parameters.forEach { p -> if (p is PathParam) p.preventMutation() }
    }

    fun createActionFor(template: RestCallAction, target: RestCallAction): RestCallAction {
        val res = template.copy() as RestCallAction
        randomizeActionGenes(res)
        res.auth = target.auth
        res.bindToSamePathResolution(target)

        return res
    }

    /**
     * Make sure that what returned is different from the target
     */
    private fun chooseClosestAncestor(target: RestCallAction, verbs: List<HttpVerb>): RestCallAction? {

        var others = sameOrAncestorEndpoints(target.path)
        others = hasWithVerbs(others, verbs).filter { t ->  t.getName() != target.getName() }

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

    private fun sameEndpoints(path: RestPath): List<RestCallAction> {

        return actionCluster.values.asSequence()
                .filter { a -> a is RestCallAction && a.path.isEquivalent(path) }
                .map { a -> a as RestCallAction }
                .toList()
    }

    private fun initAdHocInitialIndividuals() {

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

}