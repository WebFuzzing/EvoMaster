package org.evomaster.core.problem.rest.service

import com.google.inject.Inject
import org.evomaster.clientJava.controllerApi.dto.SutInfoDto
import org.evomaster.core.problem.rest.*
import org.evomaster.core.problem.rest.auth.NoAuth
import org.evomaster.core.problem.rest.param.BodyParam
import org.evomaster.core.problem.rest.param.FormParam
import org.evomaster.core.search.ActionResult
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.FitnessValue
import org.evomaster.core.search.service.FitnessFunction
import org.evomaster.core.problem.rest.service.RemoteController
import org.glassfish.jersey.client.HttpUrlConnectorProvider
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.annotation.PostConstruct
import javax.ws.rs.client.Client
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.client.Entity
import javax.ws.rs.core.MediaType


class RestFitness : FitnessFunction<RestIndividual>() {

    companion object{
        private val log: Logger = LoggerFactory.getLogger(RestFitness::class.java)
    }

    @Inject
    private lateinit var rc: RemoteController

    private val client: Client = ClientBuilder.newClient()

    private lateinit var infoDto: SutInfoDto


    @PostConstruct
    private fun initialize() {

        //workaround bug in Jersey client
        client.property(HttpUrlConnectorProvider.SET_METHOD_WORKAROUND, true)

        val started = rc.startSUT()
        if (!started) {
            throw IllegalStateException("Cannot communicate with remote REST controller")
        }

        infoDto = rc.getSutInfo() ?: throw IllegalStateException("Cannot retrieve SUT info")
    }


    override fun calculateCoverage(individual: RestIndividual): EvaluatedIndividual<RestIndividual> {

        rc.resetSUT()

        val actionResults : MutableList<ActionResult> = mutableListOf()

        //run the test
        individual.actions.forEach({ a ->
            if (a is RestCallAction) {
                handleRestCall(a, actionResults)
            } else {
                throw IllegalStateException("Cannot handle: " + a.javaClass)
            }
        })

        /*
            We cannot request all non-covered targets, because:
            1) performance hit
            2) might not be possible to have a too long URL
         */
        val ids = randomness.choose(archive.notCoveredTargets(), 100)


        val dto = rc.getTargetCoverage(ids) ?:
                throw IllegalStateException("Cannot retrieve coverage")

        val fv = FitnessValue()

        dto.targets.forEach { t ->

            if(t.descriptiveId != null) {
                idMapper.addMapping(t.id, t.descriptiveId)
            }

            fv.updateTarget(t.id, t.value)
        }

        handleResponseTargets(fv, individual.actions, actionResults)

        return EvaluatedIndividual(fv, individual.copy() as RestIndividual, actionResults)
    }

    /**
     * Create local targets for each HTTP status code in each
     * API entry point
     */
    private fun handleResponseTargets(
            fv: FitnessValue,
            actions: MutableList<RestAction>,
            actionResults: MutableList<ActionResult>) {

        (0..actions.size-1)
                .filter { actions[it] is RestCallAction }
                .filter { actionResults[it] is RestCallResult}
                .forEach {
                    val status = (actionResults[it] as RestCallResult)
                            .getStatusCode() ?: -1
                    val desc = "$status:${actions[it].getName()}"
                    val id = idMapper.handleLocalTarget(desc)
                    fv.updateTarget(id, 1.0)
                }
    }


    private fun handleRestCall(a: RestCallAction, actionResults: MutableList<ActionResult>) {

        val path = a.path.resolve(a.parameters)
        assert(path.startsWith("/"))

        var baseUrl = infoDto.baseUrlOfSUT
        if(baseUrl.endsWith("/")){
            baseUrl = baseUrl.substring(0, baseUrl.length-1)
        }

        val builder = client.target(baseUrl + path).request()

        if(a.auth !is NoAuth){
            a.auth.headers.forEach { h ->
                builder.header(h.name, h.value)
            }
        }

        /*
            TODO: When handling headers, check that they do not
            conflict with the auth ones
         */

        /*
            TODO: need to handle "accept" of returned resource
         */

        /*
           TODO: need to handle also other formats in the body,
           not just JSON and forms
         */
        val body = a.parameters.find { p -> p is BodyParam }
        val forms = a.parameters.filter { p -> p is FormParam }.map { p -> p.gene.getValueAsString()}
                .joinToString("&")

        val bodyEntity = when{
            body != null ->  Entity.json(body.gene.getValueAsString())
            ! forms.isBlank() -> Entity.entity(forms, MediaType.APPLICATION_FORM_URLENCODED_TYPE)
            else -> Entity.json("") //FIXME
        }


        val invocation = when (a.verb) {
            HttpVerb.GET -> builder.buildGet()
            HttpVerb.POST -> builder.buildPost(bodyEntity)
            HttpVerb.PUT -> builder.buildPut(bodyEntity)
            HttpVerb.DELETE -> builder.buildDelete()
            HttpVerb.OPTIONS -> builder.build("OPTIONS")
            HttpVerb.PATCH -> builder.build("PATCH")
            HttpVerb.HEAD -> builder.build("HEAD")
        }

        val response = invocation.invoke()

        val rcr = RestCallResult()
        rcr.setStatusCode(response.status)

        if(response.hasEntity()){
            if(response.mediaType != null){
                rcr.setBodyType(response.mediaType)
            }
            try{
                val body = response.readEntity(String::class.java)
                rcr.setBody(body)
            } catch (e: Exception){
                log.warn("Failed to parse HTTP response: ${e.message}")
            }

        }

        actionResults.add(rcr)
    }
}