package org.evomaster.core.problem.rest.service

import io.swagger.models.HttpMethod
import io.swagger.models.Operation
import io.swagger.models.Swagger
import io.swagger.models.parameters.AbstractSerializableParameter
import io.swagger.models.parameters.BodyParameter
import io.swagger.parser.SwaggerParser
import org.evomaster.clientJava.controllerApi.SutInfoDto
import org.evomaster.core.problem.rest.*
import org.evomaster.core.problem.rest.param.*
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.service.Sampler
import javax.annotation.PostConstruct
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response


class RestSampler : Sampler<RestIndividual>() {

    @PostConstruct
    private fun initialize() {

        val rc = RemoteController(configuration.sutControllerHost, configuration.sutControllerPort)

        val started = rc.startSUT()
        if (!started) {
            throw IllegalStateException("Cannot communicate with remote REST controller")
        }

        val infoDto = rc.getSutInfo() ?: throw IllegalStateException("Cannot retrieve SUT info")

        val swagger = getSwagger(infoDto)

        createActions(swagger)

        rc.close()
    }


    internal fun createActions(swagger: Swagger) {

        actionCluster.clear()

        //TODO check Swagger version

        swagger.paths.forEach { e ->

            val restPath = RestPath((swagger.basePath ?: "") + "/" + e.key)

            e.value.operationMap.forEach { o ->
                val verb = HttpVerb.from(o.key)

                val params = extractParams(o, swagger)

                val action = RestCallAction(verb, restPath, params)

                actionCluster.put(action.getName(), action)
            }
        }

    }



    private fun extractParams(o: Map.Entry<HttpMethod, Operation>, swagger: Swagger)
            : MutableList<Param> {

        val params: MutableList<Param> = mutableListOf()

        o.value.parameters.forEach { p ->

            val name = p.name ?: "undefined"


            if (p is AbstractSerializableParameter<*>) {
                //TODO: int64, double and float, and constraints
                //TODO: format is optional, but type is mandatory
                //TODO: see http://swagger.io/specification/

                var gene = getGene(name, p.getType(), p.getFormat())
                if (!p.required) {
                    gene = OptionalGene(name, gene)
                }

                when (p.`in`) {
                    "query" -> params.add(QueryParam(name, gene))
                    "path" -> params.add(PathParam(name, gene))
                    "header" -> throw IllegalStateException("TODO header")
                    "formData" -> params.add(FormParam(name, gene))
                    else -> throw IllegalStateException("Unrecognized: " + p.getIn())
                }

            } else if (p is BodyParameter) {

                val ref = p.schema.reference
                val classDef = ref.substring(ref.lastIndexOf("/") + 1)

                val model = swagger.definitions[classDef] ?:
                        throw IllegalStateException("No $classDef among the object definitions")

                val fields: MutableList<Gene> = mutableListOf()

                model.properties.entries.forEach { o ->
                    fields.add(getGene(o.key, o.value.type, o.value.format))
                }

                params.add(BodyParam(ObjectGene("body", fields)))
            }
        }

        return params
    }

    /**
     * type is mandatory, whereas format is optional
     */
    private fun getGene(name: String, type: String, format: String?): Gene {

        /*
        http://swagger.io/specification/#dataTypeFormat

        Common Name	    type	format	Comments
        integer	        integer	int32	signed 32 bits
        long	        integer	int64	signed 64 bits
        float	        number	float
        double	        number	double
        string	        string
        byte	        string	byte	base64 encoded characters
        binary	        string	binary	any sequence of octets
        boolean	        boolean
        date	        string	date	As defined by full-date - RFC3339
        dateTime	    string	date-time	As defined by date-time - RFC3339
        password	    string	password	Used to hint UIs the input needs to be obscured.

        TODO all cases, and constraints
        */

        //first check for format
        when (format) {
            "int32" -> return IntegerGene(name)
            "int64" -> return LongGene(name)
        }

        when (type) {
            "boolean" -> return BooleanGene(name)
            "string" -> return StringGene(name)
        }

        throw IllegalArgumentException("Cannot handle combination $type/$format")
    }


    private fun getSwagger(infoDto: SutInfoDto): Swagger {

        val swaggerURL = infoDto.swaggerJsonUrl ?:
                throw IllegalStateException("Cannot retrieve Swagger URL")

        val response = ClientBuilder.newClient()
                .target(swaggerURL)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get()

        if (!response.statusInfo.family.equals(Response.Status.Family.SUCCESSFUL)) {
            throw IllegalStateException("Cannot retrieve Swagger JSON data")
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

        val action = randomness.choose(actionCluster).copy()

        action.seeGenes().forEach { g -> g.randomize(randomness, false) }

        return RestIndividual(mutableListOf(action as RestAction))
    }
}