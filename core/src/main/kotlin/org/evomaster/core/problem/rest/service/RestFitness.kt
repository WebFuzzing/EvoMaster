package org.evomaster.core.problem.rest.service

import org.evomaster.clientJava.controllerApi.SutInfoDto
import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.core.problem.rest.RemoteController
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.problem.rest.param.BodyParam
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.FitnessValue
import org.evomaster.core.search.service.FitnessFunction
import javax.ws.rs.client.Client
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.client.Entity


class RestFitness : FitnessFunction<RestIndividual>() {

    /*
        FIXME: this is a workaround for Guice, as it
        does not support @PostConstruct :(
        should really switch to CDI Weld or Spring
     */
    var initialized = false
        private set

    private val client: Client = ClientBuilder.newClient()

    private lateinit var rc: RemoteController

    private lateinit var infoDto: SutInfoDto


    fun initialize() {
        if (initialized) {
            return
        }

        rc = RemoteController(configuration.sutControllerHost, configuration.sutControllerPort)

        val started = rc.startSUT()
        if (!started) {
            throw IllegalStateException("Cannot communicate with remote REST controller")
        }

        infoDto = rc.getInfo() ?: throw IllegalStateException("Cannot retrieve SUT info")

        initialized = true
    }


    override fun calculateCoverage(individual: RestIndividual): EvaluatedIndividual<RestIndividual> {

        initialize()

        rc.resetSUT()

        //run the test
        individual.actions.forEach({ a ->
            if (a is RestCallAction) {

                val baseUrl = infoDto.baseUrlOfSUT
                val path = a.path.resolve(a.parameters)

                var builder = client.target(baseUrl + "/" + path).request()

                /*
                    TODO: need to handle also other formats, not just JSON
                 */
                val body = a.parameters.find { p -> p is BodyParam }
                val bodyEntity = if (body != null) {
                    Entity.json(body.gene.getValueAsString())
                } else {
                    Entity.json("")
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

                //TODO objectives for response, eg status
                // likely we ll need to use negative ids

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

        //TODO need to store the HTTP response for assertions

        return EvaluatedIndividual(fv, individual.copy() as RestIndividual)
    }
}