package org.evomaster.core.llm

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import dev.langchain4j.model.chat.ChatModel
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.parameters.RequestBody
import org.evomaster.core.problem.rest.schema.OpenApiAccess
import org.evomaster.core.utils.TimeUtils
import java.io.File
import java.util.Collections
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.io.path.Path


object DictionaryCreator {

    private data class FieldInfo(
        val name: String,
        val description: String?
    )

    @JvmStatic
    fun main(args: Array<String>) {

        val API_KEY = ""

        val file = File("src/main/resources/llm_dictionary.jsonl")
        val errors = 2 //could had handled them, but too late
        val alreadyHandled = errors + file.bufferedReader().use { it.lineSequence().count() }

//        val modelName = "deepseek-v4-pro"
        val modelName = "deepseek-v4-flash"
        val model = LlmSupport.createModel(LlmProvider.DEEPSEEK, modelName = modelName, apiKey = API_KEY)

        val data = createNameDescriptions()

        TimeUtils.measureTimeMillis(
            {ms, res -> println("Took ${ms/1000}s")},
            {
                val parallelism = 20
                val executor = Executors.newFixedThreadPool(parallelism)
                val list = Collections.synchronizedList(mutableListOf<String>())

                val futures = data.entries
                    .sortedBy { it.key }
                    .drop(alreadyHandled)
                    .take(500) //TODO use for incremental job
                    .map { entry ->
                        executor.submit { handleEntry(entry, model, list) }
                    }
                futures.forEach {
                    try {
                        it.get(120, TimeUnit.SECONDS)
                    }catch (e: Exception) {
                        println("ERROR. Failed to wait for job: ${e.message}")
                    }
                }
                executor.shutdown()

                list.sorted().forEach {
                    file.appendText("$it\n")
                }
            })
    }

    private fun handleEntry(
        entry: Map.Entry<String, Set<String>>,
        model: ChatModel,
        buffer: MutableList<String>
    ) {
        val mapper = ObjectMapper()
        val name = entry.key
        val description = entry.value.maxByOrNull { it.length }
        val prompt = Prompts.getPromptForNameDescription(name, description)
        var result = LlmSupport.chat(model, prompt.first, prompt.second)
        try {
            mapper.readValue(result, object : TypeReference<List<String>>() {})
        } catch (e: Exception) {
            print("ERROR. Failed handling response: $result")
            try {
                val failed = Prompts.getPromptForFailedName(e.toString())
                result = LlmSupport.chat(model, failed.first, failed.second)
                mapper.readValue(result, object : TypeReference<List<String>>() {})
            } catch (e: Exception) {
                print("ERROR. Failed again handling response: $result")
                throw e
            }
        }

        //must be a single row, and we have it in the prompt, but sometimes it is ignored
        val row = "{ \"$name\": $result }".replace('\n',' ')
        println(row)
        buffer.add(row)
    }

    fun createNameDescriptions() : Map<String, Set<String>> {

        val data = mutableMapOf<String, MutableSet<String>>()

        val locations = scanForSchemas("./src/test/resources/swagger")
            .plus(scanForSchemas("../core-tests/integration-tests/core-it/src/test/resources/APIs_guru"))

        for(l in locations){
            println("Analyzing: $l")
            val schema = try{
                OpenApiAccess.getOpenAPIFromLocation(l)
            } catch (e: Exception){
                println("Failed to analyze: $l")
                continue
            }

            val params = extractStringFieldInfo(schema.schemaParsed)
            for(p in params){
                val descriptions = data.getOrPut(p.name.lowercase()) { mutableSetOf() }
                if(p.description != null){
                    descriptions.add(p.description)
                }
            }
        }

        println("Obtained ${data.size} field info")
        println("Total number of descriptions: ${data.values.sumOf { it.size }}")
        val maxDsc = data.maxBy { it.value.size }
        println("Most descriptions are for '${maxDsc.key}' with ${maxDsc.value.size} distinct entries")
        return data
    }

    private fun scanForSchemas(relativePath: String) : List<String>{
        val target = File(relativePath)
        if (!target.exists()) {
            throw IllegalStateException("OpenAPI resource folder does not exist: ${target.absolutePath}")
        }

        return target.walk()
            .filter { it.isFile }
            .map {
                val s = Path(it.absolutePath).toAbsolutePath().normalize().toString()
                s
            }.toList()
    }


    private fun extractStringFieldInfo(openAPI: OpenAPI): List<FieldInfo> {
        val result = mutableListOf<FieldInfo>()

        // Process schemas/components
        openAPI.components?.schemas?.forEach { (schemaName, schema) ->
            extractStringFieldsFromSchema(schema, schemaName, result)
        }

        // Process paths (parameters and request bodies)
        openAPI.paths?.forEach { (path, pathItem) ->
            pathItem.readOperations().forEach { operation ->
                operation.parameters?.forEach { parameter ->
                    if (isStringParameter(parameter)) {
                        result.add(FieldInfo(
                            name = parameter.name ?: "unknown",
                            description = parameter.description
                        ))
                    }
                }

                // Request body
                operation.requestBody?.let { requestBody ->
                    extractStringFieldsFromRequestBody(requestBody, result)
                }
            }
        }

        return result
    }

    private fun extractStringFieldsFromSchema(
        schema: Schema<*>,
        fieldName: String,
        result: MutableList<FieldInfo>
    ) {
        if (isStringSchema(schema)) {
            result.add(FieldInfo(
                name = fieldName,
                description = schema.description
            ))
        }

        // Recursively process properties
        schema.properties?.forEach { (propName, propSchema) ->
            if (isStringSchema(propSchema)) {
                result.add(FieldInfo(
                    name = propName,
                    description = propSchema.description
                ))
            }

            if (propSchema.properties != null) {
                extractStringFieldsFromSchema(propSchema, propName, result)
            }

            if (propSchema.type == "array" && propSchema.items != null) {
                if (isStringSchema(propSchema.items)) {
                    result.add(FieldInfo(
                        name = propName,
                        description = propSchema.items.description
                    ))
                }
            }
        }
    }

    private fun extractStringFieldsFromRequestBody(
        requestBody: RequestBody,
        result: MutableList<FieldInfo>
    ) {
        requestBody.content?.forEach { (_, mediaType) ->
            mediaType.schema?.let { schema ->
                extractStringFieldsFromSchema(schema, "body", result)
            }
        }
    }

    private fun isStringParameter(parameter: Parameter): Boolean {
        return when {
            parameter.schema != null && isStringSchema(parameter.schema) -> true

            parameter.content != null -> {
                parameter.content.any { (_, mediaType) ->
                    mediaType.schema != null && isStringSchema(mediaType.schema)
                }
            }

            else -> false
        }
    }

    private fun isStringSchema(schema: Schema<*>): Boolean {
        return schema.type == "string" ||
                schema.format == "string" ||
                (schema.type == null && schema.properties == null && schema.items == null &&
                        schema.`$ref` == null)
    }
}