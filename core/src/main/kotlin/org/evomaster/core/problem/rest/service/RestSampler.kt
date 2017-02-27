package org.evomaster.core.problem.rest.service

import com.google.inject.Inject
import io.swagger.models.Swagger
import io.swagger.parser.SwaggerParser
import org.evomaster.clientJava.controllerApi.dto.SutInfoDto
import org.evomaster.core.problem.rest.*
import org.evomaster.core.problem.rest.auth.AuthenticationHeader
import org.evomaster.core.problem.rest.auth.AuthenticationInfo
import org.evomaster.core.problem.rest.auth.NoAuth
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

        //TODO: for now, we just consider one single action per individual

        val action = sampleRandomAction(0.05)

        return RestIndividual(mutableListOf(action))
    }


    private fun randomizeActionGenes(action: Action) {
        action.seeGenes().forEach { g -> g.randomize(randomness, false) }
    }


    private fun sampleRandomAction(noAuthP: Double): RestAction {
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
            return RestIndividual(mutableListOf(action))
        }

        if (config.maxTestSize <= 1) {
            return sampleAtRandom()
        }


        val test = mutableListOf<RestAction>()

        val action = sampleRandomCallAction(0.0)

        when (action.verb) {
            HttpVerb.GET ->  handleSmartGet(action, test)
            HttpVerb.POST -> return RestIndividual(mutableListOf(action))
            HttpVerb.PUT -> handleSmartPut(action, test)
            //TODO DELETE
            //TODO PATCH
        }

        if (!test.isEmpty()) {
            return RestIndividual(test)
        }

        return sampleAtRandom()
    }

    private fun handleSmartPut(action: RestCallAction, test: MutableList<RestAction>) {

        /*
            A PUT might be used to update an existing resource, or to create a new one
         */
        if(randomness.nextBoolean()){
            test.add(action)
            return
        }

        //TODO creation with POST
        val others = sameEndpoints(action.path)
        val postOnSamePath = hasWithVerbs(others, listOf(HttpVerb.POST))
        if(! postOnSamePath.isEmpty()){
            //possible to do a direct POST on the resource
            //TODO
        } else {
            //TODO need to find parent collection
        }

    }

    private fun handleSmartGet(action: RestCallAction, test: MutableList<RestAction>) {

        // get on a single resource, or a collection?
        val path = action.path
        val others = sameEndpoints(path)

        val createActions = hasWithVerbs(others, listOf(HttpVerb.POST, HttpVerb.PUT))
        if (!createActions.isEmpty()) {
            //can try to create elements

            val chosen = randomness.choose(createActions)

            when (chosen.verb) {
                HttpVerb.POST -> {
                    if (!path.isLastElementAParameter()) {
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
                            val create = chosen.copy() as RestCallAction
                            randomizeActionGenes(create)
                            create.auth = action.auth
                            create.bindToSamePathResolution(action)
                            test.add(create)
                        }
                        test.add(action)
                    } else {
                        /*
                           A POST on a ../{var} is weird, as one would rather expect
                           a PUT there. However, could still be feasible
                         */
                        val create = chosen.copy() as RestCallAction
                        randomizeActionGenes(create)
                        create.auth = action.auth
                        create.bindToSamePathResolution(action)
                        test.add(create)
                        test.add(action)
                    }
                }
                HttpVerb.PUT -> {
                    val create = chosen.copy() as RestCallAction
                    randomizeActionGenes(create)
                    create.auth = action.auth
                    create.bindToSamePathResolution(action)
                    test.add(create)
                    test.add(action)
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
            //TODO
        }
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