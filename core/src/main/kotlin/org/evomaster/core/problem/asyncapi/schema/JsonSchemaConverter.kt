package org.evomaster.core.problem.asyncapi.schema

import com.fasterxml.jackson.databind.JsonNode
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.BooleanSchema
import io.swagger.v3.oas.models.media.ComposedSchema
import io.swagger.v3.oas.models.media.IntegerSchema
import io.swagger.v3.oas.models.media.NumberSchema
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import org.evomaster.core.remote.SutProblemException

/**
 * Converts an AsyncAPI 3.0 message payload (which is plain JSON Schema) into
 * the Swagger media [Schema] tree that [org.evomaster.core.problem.rest.builder.RestActionBuilderV3]
 * already knows how to map onto Genes.
 *
 * This is the load-bearing reuse hook described in the thesis plan: the
 * starter slice deliberately does not duplicate the ~800 LOC schema-to-gene
 * pipeline.  Instead, all AsyncAPI message schemas are translated to
 * [Schema] here and fed to `RestActionBuilderV3.getGene` in the builder
 * layer.
 *
 * Scope: type/format/enum/properties/required/items/additionalProperties
 * plus oneOf/anyOf/allOf and `$ref` (rewritten so the rest builder treats
 * them as plain refs).  Exotic JSON Schema features (conditional schemas,
 * `if`/`then`/`else`, `not`, `dependencies`) are out of scope and trigger a
 * SutProblemException pointing the user at the limitation.
 */
object JsonSchemaConverter {

    private const val ASYNC_LOCAL_REF_PREFIX = "#/components/schemas/"
    /**
     * The AsyncAPI builder rewrites refs into the OpenAPI shape so the rest
     * builder can resolve them out of a synthetic OpenAPI doc populated with
     * the same component schemas.
     */
    private const val OPENAPI_LOCAL_REF_PREFIX = "#/components/schemas/"

    fun convert(node: JsonNode): Schema<*> = convertNode(node)

    private fun convertNode(node: JsonNode): Schema<*> {
        // $ref shortcut — rewrite to OpenAPI-shaped ref.
        node.get("\$ref")?.asText()?.let { ref ->
            val schemaId = ref.removePrefix(ASYNC_LOCAL_REF_PREFIX)
            val rewritten = OPENAPI_LOCAL_REF_PREFIX + schemaId
            return ObjectSchema().apply { this.`$ref` = rewritten }
        }

        // oneOf / anyOf / allOf
        listOf("oneOf", "anyOf", "allOf").forEach { kw ->
            node.get(kw)?.let { variants ->
                val composed = ComposedSchema()
                val converted = variants.map { convertNode(it) }
                when (kw) {
                    "oneOf" -> composed.oneOf = converted
                    "anyOf" -> composed.anyOf = converted
                    "allOf" -> composed.allOf = converted
                }
                copyDescription(node, composed)
                return composed
            }
        }

        val type = node.get("type")?.asText()
        val schema: Schema<*> = when (type) {
            "string" -> buildString(node)
            "integer" -> IntegerSchema().also { copyNumericFacets(node, it) }
            "number" -> NumberSchema().also { copyNumericFacets(node, it) }
            "boolean" -> BooleanSchema()
            "array" -> buildArray(node)
            "object", null -> buildObject(node)
            else -> throw SutProblemException(
                "Unsupported JSON Schema type '$type' in AsyncAPI message payload"
            )
        }
        copyEnum(node, schema)
        copyDescription(node, schema)
        return schema
    }

    private fun buildString(node: JsonNode): StringSchema {
        val schema = StringSchema()
        node.get("format")?.asText()?.let { schema.format = it }
        node.get("minLength")?.asInt()?.let { schema.minLength = it }
        node.get("maxLength")?.asInt()?.let { schema.maxLength = it }
        node.get("pattern")?.asText()?.let { schema.pattern = it }
        return schema
    }

    private fun buildArray(node: JsonNode): ArraySchema {
        val schema = ArraySchema()
        node.get("items")?.let { schema.items = convertNode(it) }
        node.get("minItems")?.asInt()?.let { schema.minItems = it }
        node.get("maxItems")?.asInt()?.let { schema.maxItems = it }
        node.get("uniqueItems")?.asBoolean()?.let { schema.uniqueItems = it }
        return schema
    }

    @Suppress("UNCHECKED_CAST")
    private fun buildObject(node: JsonNode): Schema<*> {
        val schema = ObjectSchema()
        val properties = mutableMapOf<String, Schema<*>>()
        node.get("properties")?.fields()?.forEach { (k, v) -> properties[k] = convertNode(v) }
        if (properties.isNotEmpty()) {
            schema.properties = properties as Map<String, Schema<Any>>
        }
        node.get("required")?.let { req ->
            if (req.isArray) schema.required = req.map { it.asText() }
        }
        node.get("additionalProperties")?.let { addl ->
            when {
                addl.isBoolean -> schema.additionalProperties = addl.asBoolean()
                addl.isObject -> schema.additionalProperties = convertNode(addl)
            }
        }
        return schema
    }

    private fun copyNumericFacets(node: JsonNode, schema: Schema<*>) {
        node.get("minimum")?.let { schema.minimum = it.decimalValue() }
        node.get("maximum")?.let { schema.maximum = it.decimalValue() }
        node.get("exclusiveMinimum")?.takeIf { it.isBoolean }?.asBoolean()?.let { schema.exclusiveMinimum = it }
        node.get("exclusiveMaximum")?.takeIf { it.isBoolean }?.asBoolean()?.let { schema.exclusiveMaximum = it }
        node.get("multipleOf")?.decimalValue()?.let { schema.multipleOf = it }
    }

    private fun copyEnum(node: JsonNode, schema: Schema<*>) {
        node.get("enum")?.let { enumNode ->
            if (!enumNode.isArray) return@let
            // Schema.setEnum is generic over the schema type; we erase to Any
            // because AsyncAPI enums can mix scalars (string vs integer in
            // discriminated oneOf payloads).
            val values = enumNode.map { primitiveValue(it) }
            @Suppress("UNCHECKED_CAST")
            (schema as Schema<Any>).setEnum(values)
        }
    }

    private fun copyDescription(node: JsonNode, schema: Schema<*>) {
        node.get("description")?.asText()?.let { schema.description = it }
        node.get("title")?.asText()?.let { schema.title = it }
    }

    private fun primitiveValue(node: JsonNode): Any = when {
        node.isTextual -> node.asText()
        node.isInt -> node.intValue()
        node.isLong -> node.longValue()
        node.isFloatingPointNumber -> node.doubleValue()
        node.isBoolean -> node.asBoolean()
        node.isNull -> "null"
        else -> node.toString()
    }
}
