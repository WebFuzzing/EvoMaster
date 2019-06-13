package org.evomaster.core.problem.rest

import io.swagger.models.*
import io.swagger.models.parameters.AbstractSerializableParameter
import io.swagger.models.parameters.BodyParameter
import io.swagger.models.parameters.Parameter
import io.swagger.models.properties.*
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.problem.rest.param.*
import org.evomaster.core.remote.SutProblemException
import org.evomaster.core.search.Action
import org.evomaster.core.search.gene.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicInteger


class RestActionBuilder {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(RestActionBuilder::class.java)
        private val idGenerator = AtomicInteger()

        fun addActionsFromSwagger(swagger: Swagger,
                                  actionCluster: MutableMap<String, Action>,
                                  endpointsToSkip: List<String> = listOf(),
                                  doParserDescription : Boolean = false) {

            actionCluster.clear()

            val version = swagger.swagger
            if (version != "2.0") {
                throw SutProblemException("EvoMaster does not support Swagger version '$version'")
            }

            val skipped = mutableListOf<String>()

            swagger.paths
                    .filter { e ->
                        if (endpointsToSkip.contains(e.key)) {
                            skipped.add(e.key)
                            false
                        } else {
                            true
                        }
                    }
                    .forEach { e ->

                        val restPath = RestPath((swagger.basePath ?: "") + "/" + e.key)

                        e.value.operationMap.forEach { o ->
                            val verb = HttpVerb.from(o.key)

                            val params = extractParams(o, swagger, restPath)

                            repairParams(params, restPath)

                            val action = RestCallAction("$verb$restPath${idGenerator.incrementAndGet()}", verb, restPath, params)
                            if(doParserDescription) {
                                var info = o.value.description
                                if(!info.isNullOrBlank() && !info.endsWith(".")) info += "."
                                if(!o.value.summary.isNullOrBlank()) info = if(info == null) o.value.summary else (info + o.value.summary)
                                if(!info.isNullOrBlank() && !info.endsWith(".")) info += "."
                                action.initTokens(info)
                            }
                            actionCluster.put(action.getName(), action)
                        }
                    }

            if (skipped.size != endpointsToSkip.size) {
                val msg = "${endpointsToSkip.size} were set to be skipped, but only ${skipped.size}" +
                        " were found in the schema"
                LoggingUtil.getInfoLogger().apply {
                    error(msg)
                    endpointsToSkip.filter { !skipped.contains(it) }
                            .forEach { warn("Missing endpoint: $it") }
                }
                throw SutProblemException(msg)
            }

            LoggingUtil.getInfoLogger().apply {

                if (skipped.size != 0) {
                    info("Skipped ${skipped.size} path endpoints from the schema configuration")
                }

                val n = actionCluster.size
                when (n) {
                    0 -> warn("There is _NO_ usable RESTful API endpoint defined in the schema configuration")
                    1 -> info("There is only one usable RESTful API endpoint defined in the schema configuration")
                    else -> info("There are $n usable RESTful API endpoints defined in the schema configuration")
                }
            }
        }

        /**
         * Have seen some cases of (old?) Swagger wrongly marking path params as query params
         */
        private fun repairParams(params: MutableList<Param>, restPath: RestPath) {

            restPath.getVariableNames().forEach { n ->

                val p = params.find { p -> p is PathParam && p.name == n }
                if (p == null) {
                    log.warn("No path parameter for variable '$n'")

                    //this could happen if bug in Swagger
                    var fixed = false
                    for (i in 0 until params.size) {
                        if (params[i] is QueryParam && params[i].name == n) {
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
                opEntry: Map.Entry<HttpMethod, Operation>,
                swagger: Swagger,
                restPath: RestPath
        ): MutableList<Param> {

            val params: MutableList<Param> = mutableListOf()
            val operation = opEntry.value

            removeDuplicatedParams(operation.parameters)
                    .forEach { p ->

                        val name = p.name ?: "undefined"

                        if (p is AbstractSerializableParameter<*>) {

                            val type = p.getType() ?: run {
                                log.warn("Missing/invalid type for '$name' in Swagger file. Using default 'string'")
                                "string"
                            }

                            var gene = getGene(name, type, p.getFormat(), swagger, null, p)
                            if (!p.required && p.`in` != "path") {
                                /*
                                    Even if a "path" parameter might not be required, still
                                    do not use an optional for it. Otherwise, might
                                    end up in quite a few useless 405 errors.

                                    Furthermore, "path" parameters must be "required" according
                                    to specs.
                                    TODO: could issue warning that Swagger is incorrect
                                */
                                gene = OptionalGene(name, gene)
                            }

                            //TODO could exploit "x-example" if available in Swagger

                            when (p.`in`) {
                                "query" -> params.add(QueryParam(name, gene))
                                "path" -> params.add(PathParam(name, DisruptiveGene("d_", gene, 1.0)))
                                "header" -> params.add(HeaderParam(name, gene))
                                "formData" -> params.add(FormParam(name, gene))
                                else -> throw IllegalStateException("Unrecognized: ${p.getIn()}")
                            }

                        } else if (p is BodyParameter
                                && !shouldAvoidCreatingObject(p, swagger)
                                && opEntry.key != HttpMethod.GET
                        ) {

                            val name = "body"

                            var gene = p.schema.reference?.let { createObjectFromReference(name, it, swagger) }
                                    ?: (p.schema as ModelImpl).let {
                                        if (it.type == "object") {
                                            createObjectFromModel(p.schema, "body", swagger, it.type)
                                        } else {
                                            getGene(name, it.type, it.format, swagger)
                                        }
                                    }

                            if (!p.required) {
                                gene = OptionalGene(name, gene)
                            }

                            var types = operation.consumes
                            if (types == null || types.isEmpty()) {
                                val msg = "Missing consume types in body payload definition. Defaulting to JSON. Endpoint: ${opEntry.key} $restPath}"
                                log.warn(msg)
                                types = listOf("application/json")
                            }

                            val contentTypeGene = EnumGene<String>("contentType", types)

                            params.add(BodyParam(gene, contentTypeGene))
                        }
                    }

            return params
        }

        /**
         *  Workaround for bug in Springfox
         */
        private fun shouldAvoidCreatingObject(p: BodyParameter, swagger: Swagger): Boolean {

            var ref: String = p.schema.reference ?: return false
            val classDef = ref.substring(ref.lastIndexOf("/") + 1)

            if (listOf("Principal", "WebRequest").contains(classDef)) {

                /*
                    This is/was a bug in Swagger for Spring, in which Spring request
                    handlers wrongly ended up in Swagger as body parts, albeit
                    missing from the definition list
                */

                swagger.definitions[classDef] ?: return true
            }

            return false
        }

        private fun createObjectFromReference(name: String,
                                              reference: String,
                                              swagger: Swagger,
                                              history: MutableList<String> = mutableListOf()
        ): Gene {

            if (history.contains(reference)) {
                return CycleObjectGene("Cycle for: $reference")
            }
            history.add(reference)

            //token after last /
            val classDef = reference.substring(reference.lastIndexOf("/") + 1)

            val model = swagger.definitions[classDef]
            if (model == null) {
                log.warn("No $classDef among the object definitions in the Swagger file")
                return ObjectGene(name, listOf(), classDef)
            }


            //TODO referenced types might not necessarily objects???

            return createObjectFromModel(model, name, swagger, classDef, history)
        }

        private fun createObjectFromModel(model: Model,
                                          name: String,
                                          swagger: Swagger,
                                          type: String?,
                                          history: MutableList<String> = mutableListOf())
                : Gene {

            if (model.properties != null) {
                val fields = createFields(model.properties, swagger, history)

                return ObjectGene(name, fields, type)
            }

            if (model is ModelImpl
                    && model.additionalProperties != null
                    && model.additionalProperties.type == "object") {
                val ap = model.additionalProperties
                return createMapGene(
                        name + "_map",
                        ap.type,
                        ap.format,
                        swagger,
                        ap,
                        history)
            }

            /*
                worst case, just create a map of strings.
                In JS/JSON, any object is in the end a map from strings (field names)
                to values (any type).
                If we have in Swagger an object definition, but then such definition has
                no declared field (eg, problems with swagger), then an ObjectGene would
                always be empty. Using a MapGene of strings would allow us to at least
                try to add some fields to it
             */
            return createMapGene(
                    name + "_map",
                    "string",
                    null,
                    swagger,
                    null,
                    history)
        }

        private fun createMapGene(
                name: String,
                type: String,
                format: String?,
                swagger: Swagger,
                property: Property? = null,
                history: MutableList<String> = mutableListOf()
        ): Gene {

            val template = getGene(
                    name + "_map",
                    type,
                    format,
                    swagger,
                    property,
                    null,
                    history)

            if (template is CycleObjectGene) {
                return CycleObjectGene("<map> ${template.name}")
            }

            return MapGene(name, template)
        }

        private fun createFields(properties: Map<String, Property>?,
                                 swagger: Swagger,
                                 history: MutableList<String> = mutableListOf())
                : List<out Gene> {

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

                    if (o.value is AbstractProperty && !o.value.required) {
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

            if (type == "string" && parameter?.getEnum()?.isEmpty() == false) {
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
                    log.warn("Unhandled format '$format'")
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
                    return createObjectFromReference(name, rp.`$ref`, swagger, history)
                }
                "array" -> {

                    val items = when {
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
                        throw IllegalStateException("Cannot handle object out of a property")
                    }

                    if (property is MapProperty) {
                        val ap = property.additionalProperties
                        return createMapGene(
                                name + "_map",
                                ap.type,
                                ap.format,
                                swagger,
                                ap,
                                history)
                    }


                    if (property is ObjectProperty) {

                        val fields = createFields(property.properties, swagger, history)
                        return ObjectGene(name, fields, property.type)
                    }
                }
                "file" -> return StringGene(name)
                //TODO file is a hack. I want to find a more elegant way of dealing with it (BMR)
            }

            throw IllegalArgumentException("Cannot handle combination $type/$format")
        }

        private fun removeDuplicatedParams(params: List<Parameter>): List<Parameter> {
            return params.distinctBy { it.name }
        }

        fun getModelsFromSwagger(swagger: Swagger,
                                 modelCluster: MutableMap<String, ObjectGene>) {
            modelCluster.clear()

            if (swagger.definitions != null) {
                swagger.definitions
                        .forEach {
                            val model = createObjectFromReference(it.key,
                                    it.component1(),
                                    swagger
                            )
                            when (model) {
                                //BMR: the modelCluster expects an ObjectGene. If the result is not that, it is wrapped in one.
                                is ObjectGene -> modelCluster.put(it.component1(), (model as ObjectGene))
                                is MapGene<*> -> modelCluster.put(it.component1(), ObjectGene(it.component1(), listOf(model)))
                            }

                        }
            }
        }
    }
}