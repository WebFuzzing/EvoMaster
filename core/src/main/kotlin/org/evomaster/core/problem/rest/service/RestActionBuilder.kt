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

        LoggingUtil.getInfoLogger().apply {
            val n = actionCluster.size
            when(n){
                0 -> warn("There is _NO_ RESTful API entry point defined in the Swagger configuration")
                1 -> info("There is only one RESTful API entry point defined in the Swagger configuration")
                else -> info("There are $n RESTful API entry points defined in the Swagger configuration")
            }
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
                        params[i] = PathParam(params[i].name, DisruptiveGene("d_", params[i].gene, 1.0))
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

                var gene = getGene(name, type, p.getFormat(), swagger, null, p)
                if (!p.required && p.`in` != "path") {
                    /*
                        Even if a "path" parameter might not be required, still
                        do not use an optional for it. Otherwise, might
                        end up in quite a few useless 405 errors
                     */
                    gene = OptionalGene(name, gene)
                }

                //TODO could exploit "x-example" if available in Swagger

                when (p.`in`) {
                    "query" -> params.add(QueryParam(name, gene))
                    "path" -> params.add(PathParam(name, DisruptiveGene("d_", gene, 1.0)))
                    "header" -> throw IllegalStateException("TODO header")
                    "formData" -> params.add(FormParam(name, gene))
                    else -> throw IllegalStateException("Unrecognized: ${p.getIn()}")
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

        val model = swagger.definitions[classDef]
        if(model == null){
            log.warn("No $classDef among the object definitions in the Swagger file")
            return ObjectGene(name, listOf())
        }


        //TODO referenced types might not necessarily objects???

        //TODO need to handle additionalProperties

        val fields = createFields(model.properties, swagger, history)

        return ObjectGene(name, fields)
    }

    private fun createFields(properties: Map<String,Property>?,
                             swagger: Swagger,
                             history: MutableList<String> = mutableListOf())
            : List<out Gene>{

        val fields: MutableList<Gene> = mutableListOf()

        properties?.entries?.forEach { o ->
            var gene = getGene(
                    o.key,
                    o.value.type,
                    o.value.format,
                    swagger,
                    o.value,
                    null,
                    history)

            if (gene !is CycleObjectGene) {

                if (o is AbstractProperty && !o.required) {
                    gene = OptionalGene(gene.name, gene)
                }

                fields.add(gene)
            }
        }

        return fields
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
            parameter: AbstractSerializableParameter<*>? = null,
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

        if(type == "string" && ! (parameter?.getEnum()?.isEmpty() ?: true) ) {
            //TODO enum can be for any type, not just strings
            //Besides the defined values, add one to test robustness
            return EnumGene(name, parameter!!.getEnum().apply { add("EVOMASTER") })
        }

        //first check for "optional" format
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

        /*
            If a format is not defined, the type should default to
            the JSON Schema definition
         */
        when (type) {
            "integer" -> return IntegerGene(name)
            "number" -> return DoubleGene(name)
            "boolean" -> return BooleanGene(name)
            "string" -> return StringGene(name)
            "ref" -> {
                if (property == null) {
                    //TODO somehow will need to handle it
                    throw IllegalStateException("Cannot handle ref out of a property")
                }
                val rp = property as RefProperty
                return getObjectGene(name, rp.`$ref`, swagger, history)
            }
            "array" -> {

                val items = when{
                    property != null -> (property as ArrayProperty).items
                    parameter != null -> parameter.getItems()
                    else -> throw IllegalStateException("Failed to handle array")
                }

                val template = getGene(
                        name + "_item",
                        items.type,
                        items.format,
                        swagger,
                        items,
                        null,
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
                            null,
                            history)

                    if (template is CycleObjectGene) {
                        return CycleObjectGene("<map> ${template.name}")
                    }

                    return MapGene(name, template)
                }

                if (property is ObjectProperty) {

                    val fields= createFields( property.properties, swagger, history)
                    return ObjectGene(name, fields)
                }
            }
        }

        throw IllegalArgumentException("Cannot handle combination $type/$format")
    }

}