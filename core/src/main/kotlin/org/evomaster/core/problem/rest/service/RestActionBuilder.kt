package org.evomaster.core.problem.rest.service

import io.swagger.models.HttpMethod
import io.swagger.models.Operation
import io.swagger.models.Swagger
import io.swagger.models.parameters.AbstractSerializableParameter
import io.swagger.models.parameters.BodyParameter
import io.swagger.models.properties.*
import org.evomaster.core.LoggingUtil
import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest.RestPath
import org.evomaster.core.problem.rest.param.*
import org.evomaster.core.search.Action
import org.evomaster.core.search.gene.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory


class RestActionBuilder {

    companion object{
        private val log: Logger = LoggerFactory.getLogger(RestActionBuilder::class.java)
    }

    fun createActions(swagger: Swagger, actionCluster: MutableMap<String, Action>) {

        actionCluster.clear()

        //TODO check Swagger version

        //TODO check for when swagger.paths is null

        swagger.paths.forEach { e ->

            val restPath = RestPath((swagger.basePath ?: "") + "/" + e.key)

            e.value.operationMap.forEach { o ->
                val verb = HttpVerb.from(o.key)

                val params = extractParams(o, swagger)

                repairParams(params, restPath)

                val action = RestCallAction(verb, restPath, params)

                actionCluster.put(action.getName(), action)
            }
        }

        val n = actionCluster.size
        if (n == 1) {
            LoggingUtil.getInfoLogger()
                    .info("There is only one RESTful API entry point defined in the Swagger configuration")
        } else {
            LoggingUtil.getInfoLogger()
                    .info("There are $n RESTful API entry points defined in the Swagger configuration")
        }
    }

    /**
     * Have seen some cases of (old?) Swagger wrongly marking path params as query params
     */
    private fun repairParams(params: MutableList<Param>, restPath: RestPath) {

        restPath.getVariableNames().forEach { n ->

            var p = params.find{p -> p is PathParam && p.name == n}
            if(p == null){
                log.warn("No path parameter for variable '$n'")

                //this could happen if bug in Swagger
                var fixed = false
                for(i in 0 until params.size) {
                    if(params[i] is QueryParam && params[i].name == n){
                        params[i] = PathParam(params[i].name, params[i].gene)
                        fixed = true
                        break
                    }
                }
                if (!fixed) {
                    throw IllegalArgumentException("Cannot resolve path parameter '$n'")
                }
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

                val type = p.getType() ?: run {
                    RestSampler.log.warn("Missing/invalid type for '$name' in Swagger file. Using default 'string'")
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

            if (gene !is CycleObjectGene) {
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

        */

        //first check for format
        when (format) {
            "int32" -> return IntegerGene(name)
            "int64" -> return LongGene(name)
            "double" -> return DoubleGene(name)
            "float" -> return FloatGene(name)
            "password" -> return StringGene(name) //nothing special to do, it is just a hint
            "binary" -> return StringGene(name) //does it need to be treated specially?
            "byte" -> return Base64StringGene(name)
            "date" -> return DateGene(name)
            "date-time" -> return DateTimeGene(name)
            else -> if (format != null) {
                RestSampler.log.warn("Unhandled format '$format'")
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

                if (template is CycleObjectGene) {
                    return CycleObjectGene("<array> ${template.name}")
                }

                return ArrayGene(name, template)
            }
            "object" -> {
                if (property == null) {
                    //TODO somehow will need to handle it
                    throw IllegalStateException("Cannot handle array out of a property")
                }
                if (property is MapProperty) {
                    val ap = property.additionalProperties
                    val template = getGene(
                            name + "_map",
                            ap.type,
                            ap.format,
                            swagger,
                            ap,
                            history)

                    if (template is CycleObjectGene) {
                        return CycleObjectGene("<map> ${template.name}")
                    }

                    return MapGene(name, template)
                }
                if (property is ObjectProperty) {

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

                        if (gene !is CycleObjectGene) {
                            fields.add(gene)
                        }
                    }

                    return ObjectGene(name, fields)
                }
            }
        }

        throw IllegalArgumentException("Cannot handle combination $type/$format")
    }

}