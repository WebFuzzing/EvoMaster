package org.evomaster.core.problem.rest.service

import com.google.inject.Inject
import io.swagger.models.HttpMethod
import io.swagger.models.Operation
import io.swagger.models.Swagger
import io.swagger.models.parameters.AbstractSerializableParameter
import io.swagger.models.parameters.BodyParameter
import io.swagger.models.properties.*
import io.swagger.parser.SwaggerParser
import org.evomaster.clientJava.controllerApi.dto.SutInfoDto
import org.evomaster.core.problem.rest.*
import org.evomaster.core.problem.rest.param.*
import org.evomaster.core.search.gene.*
import org.evomaster.core.problem.rest.service.RemoteController
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
    protected lateinit var rc: RemoteController


    @PostConstruct
    private fun initialize() {

        val started = rc.startSUT()
        if (!started) {
            throw IllegalStateException("Cannot communicate with remote REST controller")
        }

        val infoDto = rc.getSutInfo() ?: throw IllegalStateException("Cannot retrieve SUT info")

        val swagger = getSwagger(infoDto)

        createActions(swagger)
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


    private fun extractParams(
            o: Map.Entry<HttpMethod, Operation>,
            swagger: Swagger
    ): MutableList<Param> {

        val params: MutableList<Param> = mutableListOf()

        o.value.parameters.forEach { p ->

            val name = p.name ?: "undefined"

            if (p is AbstractSerializableParameter<*>) {
                //TODO: int64, double and float, and constraints
                //TODO: format is optional, but type is mandatory
                //TODO: see http://swagger.io/specification/

                val type = p.getType() ?: run {
                    log.warn("Missing/invalid type for '$name' in Swagger file. Using default 'string'")
                    "string"
                }

                var gene = getGene(name, type, p.getFormat(), swagger)
                if (!p.required) {
                    gene = OptionalGene(name, gene)
                }

                //TODO could exploit "x-example" if available in Swagger

                when (p.`in`) {
                    "query" -> params.add(QueryParam(name, gene))
                    "path" -> params.add(PathParam(name, gene))
                    "header" -> throw IllegalStateException("TODO header")
                    "formData" -> params.add(FormParam(name, gene))
                    else -> throw IllegalStateException("Unrecognized: " + p.getIn())
                }

            } else if (p is BodyParameter) {

                val ref = p.schema.reference

                params.add(BodyParam(
                        getObjectGene("body", ref, swagger)))
            }
        }

        return params
    }

    private fun getObjectGene(name: String,
                              reference: String,
                              swagger: Swagger,
                              history: MutableList<String> = mutableListOf()
    ): ObjectGene {

        if (history.contains(reference)) {
            return CycleObjectGene("Cycle for: $reference")
        }
        history.add(reference)

        //token after last /
        val classDef = reference.substring(reference.lastIndexOf("/") + 1)

        val model = swagger.definitions[classDef] ?:
                throw IllegalStateException("No $classDef among the object definitions")

        //TODO referenced types might not necessarily objects???

        //TODO need to handle additionalProperties

        val fields: MutableList<Gene> = mutableListOf()

        model.properties?.entries?.forEach { o ->
            val gene = getGene(
                    o.key,
                    o.value.type,
                    o.value.format,
                    swagger,
                    o.value,
                    history)

            if(gene !is CycleObjectGene) {
                fields.add(gene)
            }
        }

        return ObjectGene(name, fields)
    }

    /**
     * type is mandatory, whereas format is optional
     */
    private fun getGene(
            name: String,
            type: String,
            format: String?,
            swagger: Swagger,
            property: Property? = null,
            history: MutableList<String> = mutableListOf()
    ): Gene {

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
            "double" -> return DoubleGene(name)
            "float"  -> return FloatGene(name)
            else -> if(format!=null) {
                log.warn("Unhandled format '$format'")
            }
        }

        when (type) {
            "integer" -> return IntegerGene(name)
            "boolean" -> return BooleanGene(name)
            "string" -> return StringGene(name)
            "ref" -> {
                if (property == null) {
                    //TODO somehow will need to handle it
                    throw IllegalStateException("Cannot handle ref out of a property")
                }
                val rp = property as RefProperty
                return getObjectGene(rp.simpleRef, rp.`$ref`, swagger, history)
            }
            "array" -> {
                if (property == null) {
                    //TODO somehow will need to handle it
                    throw IllegalStateException("Cannot handle array out of a property")
                }
                val ap = property as ArrayProperty
                val items = ap.items
                val template = getGene(
                        name + "_item",
                        items.type,
                        items.format,
                        swagger,
                        items,
                        history)

                if(template is CycleObjectGene){
                    return CycleObjectGene("<array> ${template.name}")
                }

                return ArrayGene(name, template)
            }
            "object" ->{
                if (property == null) {
                    //TODO somehow will need to handle it
                    throw IllegalStateException("Cannot handle array out of a property")
                }
                if(property is MapProperty){
                    val ap = property.additionalProperties
                    val template = getGene(
                            name + "_map",
                            ap.type,
                            ap.format,
                            swagger,
                            ap,
                            history)

                    if(template is CycleObjectGene){
                        return CycleObjectGene("<map> ${template.name}")
                    }

                    return MapGene(name, template)
                }
                if(property is ObjectProperty){

                    //TODO refactor the copy&paste
                    val fields: MutableList<Gene> = mutableListOf()

                    property.properties.entries.forEach { o ->
                        val gene = getGene(
                                o.key,
                                o.value.type,
                                o.value.format,
                                swagger,
                                o.value,
                                history)

                        if(gene !is CycleObjectGene) {
                            fields.add(gene)
                        }
                    }

                    return ObjectGene(name, fields)
                }
            }
        }

        throw IllegalArgumentException("Cannot handle combination $type/$format")
    }


    private fun getSwagger(infoDto: SutInfoDto): Swagger {

        val swaggerURL = infoDto.swaggerJsonUrl ?:
                throw IllegalStateException("Cannot retrieve Swagger URL")

        val response = try {
            ClientBuilder.newClient()
                    .target(swaggerURL)
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .get()
        } catch (e: Exception){
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

        val action = randomness.choose(actionCluster).copy()

        action.seeGenes().forEach { g -> g.randomize(randomness, false) }

        return RestIndividual(mutableListOf(action as RestAction))
    }
}