package org.evomaster.core.problem.api.schema

import com.fasterxml.jackson.databind.JsonNode
import com.networknt.schema.JsonSchema
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.PathType
import com.networknt.schema.SchemaValidatorsConfig
import com.networknt.schema.SpecVersion
import com.networknt.schema.resource.DisallowSchemaLoader

/**
 * Compiles and validates JSON Schema draft 2020-12 documents.
 */
class JsonSchemaValidator(allowExternalReferences: Boolean = false) {

    private val factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012) { builder ->
        if (!allowExternalReferences) {
            // prevent schema compilation from fetching schema refs via network request
            builder.schemaLoaders { it.add(DisallowSchemaLoader.getInstance()) }
        }
    }

    fun compile(schema: JsonNode): CompilationResult {
        if (!schema.isObject && !schema.isBoolean) {
            return CompilationResult.Failure(
                listOf(CompilationIssue("json-schema:invalid-root:/", "/", "A JSON Schema must be an object or a boolean"))
            )
        }

        return try {
            val config = SchemaValidatorsConfig.builder()
                .failFast(false)
                .typeLoose(false)
                .javaSemantics(false)
                .formatAssertionsEnabled(false)
                .pathType(PathType.JSON_POINTER)
                .build()
            val compiledSchema = factory.getSchema(schema, config)
            compiledSchema.initializeValidators()
            CompilationResult.Success(CompiledSchema(compiledSchema))
        } catch (e: Exception) {
            CompilationResult.Failure(
                listOf(CompilationIssue("json-schema:invalid-schema:/", "/", e.message ?: "Unable to compile JSON Schema"))
            )
        }
    }

    class CompiledSchema internal constructor(
        private val schema: JsonSchema
    ) {
        fun validate(instance: JsonNode): List<Violation> = schema.validate(instance)
            .take(100)
            .map { message ->
                val keyword = message.messageKey.substringAfterLast('.', message.messageKey)
                val instancePointer = message.instanceLocation.toString()
                val schemaPointer = message.evaluationPath.toString()
                Violation(
                    key = "json-schema:$keyword:$instancePointer:$schemaPointer",
                    keyword = keyword,
                    message = "JSON Schema keyword '$keyword' failed at $instancePointer"
                )
            }
            .distinctBy { it.key }
            .sortedWith(compareBy<Violation>({ it.key }, { it.message }))
    }

    sealed interface CompilationResult {
        data class Success(val schema: CompiledSchema) : CompilationResult
        data class Failure(val issues: List<CompilationIssue>) : CompilationResult
    }

    /** A problem that prevents a JSON Schema from being compiled safely. */
    data class CompilationIssue(
        val key: String,
        val schemaPointer: String,
        val message: String
    )

    /** A deterministic JSON Schema instance-validation failure. */
    data class Violation(
        val key: String,
        val keyword: String,
        val message: String
    )
}
