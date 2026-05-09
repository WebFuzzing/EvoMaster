package org.evomaster.core.problem.asyncapi.schema

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import org.evomaster.core.problem.rest.schema.SchemaLocation
import org.evomaster.core.problem.rest.schema.SchemaLocationType
import org.evomaster.core.remote.SutProblemException
import org.slf4j.LoggerFactory
import java.net.ConnectException
import java.net.URI
import java.nio.file.Files
import java.nio.file.Paths
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.core.Response

/**
 * Loads and parses an AsyncAPI 3.0 schema document into the internal
 * [AsyncAPISchema] model.
 *
 * Out of scope for the starter slice: AsyncAPI 2.x (rejected explicitly with a
 * clear error so users understand what to do), authenticated schema retrieval,
 * external `$ref` documents.
 */
object AsyncAPIAccess {

    private val log = LoggerFactory.getLogger(AsyncAPIAccess::class.java)

    private val yamlMapper = ObjectMapper(YAMLFactory())
    private val jsonMapper = ObjectMapper()

    private const val LOCAL_MESSAGE_REF_PREFIX = "#/components/messages/"
    private const val LOCAL_SCHEMA_REF_PREFIX = "#/components/schemas/"
    private const val LOCAL_CHANNEL_REF_PREFIX = "#/channels/"

    /**
     * Retrieve and parse an AsyncAPI schema. The location can be a remote URL
     * in http(s), a `file://` URL, or a local OS file path.
     */
    fun getAsyncAPIFromLocation(asyncApiLocation: String): AsyncAPISchema {
        val data: String
        val location: SchemaLocation
        if (asyncApiLocation.startsWith("http", true)) {
            data = readFromRemoteServer(asyncApiLocation)
            location = SchemaLocation(asyncApiLocation, SchemaLocationType.REMOTE)
        } else {
            data = readFromDisk(asyncApiLocation)
            location = SchemaLocation(asyncApiLocation, SchemaLocationType.LOCAL)
        }
        return parse(data, location)
    }

    /** Test-only helper: load the schema from a classpath resource. */
    fun getAsyncAPIFromResource(resourceLocation: String): AsyncAPISchema {
        val data = this.javaClass.getResource(resourceLocation)?.readText()
            ?: throw SutProblemException("AsyncAPI resource not found on classpath: $resourceLocation")
        return parse(data, SchemaLocation(resourceLocation, SchemaLocationType.RESOURCE))
    }

    fun parse(schemaText: String, location: SchemaLocation): AsyncAPISchema {

        val root = readTree(schemaText)

        val versionNode = root.get("asyncapi")
            ?: throw SutProblemException("AsyncAPI schema missing top-level 'asyncapi' version field")
        val version = versionNode.asText()
        if (!version.startsWith("3.")) {
            throw SutProblemException(
                "AsyncAPI version $version is not supported in this build; only 3.x is parsed." +
                        " (AsyncAPI 2.x support is planned as a follow-up.)"
            )
        }

        val defaultContentType = root.get("defaultContentType")?.asText() ?: "application/json"
        val servers = parseServers(root.get("servers"))
        val componentMessages = parseComponentMessages(root, defaultContentType)
        val componentSchemas = parseComponentSchemas(root)
        val channels = parseChannels(root.get("channels"))
        val operations = parseOperations(root.get("operations"))

        return AsyncAPISchema(
            rawText = schemaText,
            location = location,
            version = version,
            channels = channels,
            operations = operations,
            messages = componentMessages,
            componentSchemas = componentSchemas,
            defaultContentType = defaultContentType,
            servers = servers
        )
    }

    private fun readTree(schemaText: String): JsonNode {
        val trimmed = schemaText.trimStart()
        val mapper = if (trimmed.startsWith("{")) jsonMapper else yamlMapper
        return try {
            mapper.readTree(schemaText)
        } catch (e: Exception) {
            throw SutProblemException("Failed to parse AsyncAPI schema: ${e.message}")
        }
    }

    private fun parseServers(node: JsonNode?): Map<String, AsyncAPIServer> {
        if (node == null || !node.isObject) return emptyMap()
        val out = LinkedHashMap<String, AsyncAPIServer>()
        node.fields().forEach { (name, server) ->
            val host = server.get("host")?.asText() ?: ""
            val protocol = server.get("protocol")?.asText() ?: ""
            out[name] = AsyncAPIServer(name = name, host = host, protocol = protocol)
        }
        return out
    }

    private fun parseComponentMessages(
        root: JsonNode,
        defaultContentType: String
    ): Map<String, AsyncAPIMessage> {
        val components = root.get("components") ?: return emptyMap()
        val messagesNode = components.get("messages") ?: return emptyMap()
        val out = LinkedHashMap<String, AsyncAPIMessage>()
        messagesNode.fields().forEach { (id, message) ->
            val payload = message.get("payload")
            val payloadRefRaw = payload?.get("\$ref")?.asText()
            val payloadRef = payloadRefRaw?.removePrefix(LOCAL_SCHEMA_REF_PREFIX)
                ?.takeIf { it != payloadRefRaw }
            val payloadInline = if (payload != null && payloadRef == null) payload else null

            val correlationLocation = message.get("correlationId")?.get("location")?.asText()

            val headers = message.get("headers")
            val headersRefRaw = headers?.get("\$ref")?.asText()
            val headersRef = headersRefRaw?.removePrefix(LOCAL_SCHEMA_REF_PREFIX)
                ?.takeIf { it != headersRefRaw }
            val headersInline = if (headers != null && headersRef == null) headers else null

            out[id] = AsyncAPIMessage(
                id = id,
                name = message.get("name")?.asText() ?: id,
                contentType = message.get("contentType")?.asText() ?: defaultContentType,
                correlationLocation = correlationLocation,
                payloadSchemaRef = payloadRef,
                payloadInline = payloadInline,
                headersSchemaRef = headersRef,
                headersInline = headersInline
            )
        }
        return out
    }

    private fun parseComponentSchemas(root: JsonNode): Map<String, JsonNode> {
        val components = root.get("components") ?: return emptyMap()
        val schemasNode = components.get("schemas") ?: return emptyMap()
        val out = LinkedHashMap<String, JsonNode>()
        schemasNode.fields().forEach { (id, schema) -> out[id] = schema }
        return out
    }

    private fun parseChannels(node: JsonNode?): Map<String, AsyncAPIChannel> {
        if (node == null || !node.isObject) return emptyMap()
        val out = LinkedHashMap<String, AsyncAPIChannel>()
        node.fields().forEach { (key, channel) ->
            val address = channel.get("address")?.asText()
            val messages = channel.get("messages")
            val messageIds = mutableListOf<String>()
            messages?.fields()?.forEach { (_, ref) ->
                resolveLocalRef(ref, LOCAL_MESSAGE_REF_PREFIX)?.let { messageIds.add(it) }
            }
            val parameters = LinkedHashMap<String, JsonNode>()
            channel.get("parameters")?.takeIf { it.isObject }?.fields()?.forEach { (paramName, paramNode) ->
                parameters[paramName] = paramNode
            }
            out[key] = AsyncAPIChannel(
                name = key,
                address = address,
                messageIds = messageIds,
                parameters = parameters
            )
        }
        return out
    }

    private fun parseOperations(node: JsonNode?): Map<String, AsyncAPIOperation> {
        if (node == null || !node.isObject) return emptyMap()
        val out = LinkedHashMap<String, AsyncAPIOperation>()
        node.fields().forEach { (key, op) ->
            val actionRaw = op.get("action")?.asText()
                ?: throw SutProblemException("AsyncAPI operation '$key' missing 'action'")
            val action = when (actionRaw.lowercase()) {
                "send" -> AsyncAPIOperation.Action.SEND
                "receive" -> AsyncAPIOperation.Action.RECEIVE
                else -> throw SutProblemException(
                    "Unsupported AsyncAPI operation action '$actionRaw' on '$key' " +
                            "(expected 'send' or 'receive')"
                )
            }
            val channelRef = op.get("channel")
                ?: throw SutProblemException("AsyncAPI operation '$key' missing 'channel'")
            val channelName = resolveLocalRef(channelRef, LOCAL_CHANNEL_REF_PREFIX)
                ?: throw SutProblemException("AsyncAPI operation '$key' has unresolved channel reference")

            val messageIds = collectOperationMessageIds(op.get("messages"))

            val replyNode = op.get("reply")
            val reply = if (replyNode != null) {
                val replyChannelRef = replyNode.get("channel")
                val replyChannel = replyChannelRef?.let { resolveLocalRef(it, LOCAL_CHANNEL_REF_PREFIX) }
                    ?: throw SutProblemException("AsyncAPI operation '$key' has reply with no/unresolved channel")
                ReplyBinding(
                    channelNames = listOf(replyChannel),
                    messageIds = collectOperationMessageIds(replyNode.get("messages"))
                )
            } else null

            out[key] = AsyncAPIOperation(
                name = key,
                action = action,
                channelName = channelName,
                messageIds = messageIds,
                reply = reply
            )
        }
        return out
    }

    private fun collectOperationMessageIds(messagesNode: JsonNode?): List<String> {
        if (messagesNode == null || !messagesNode.isArray) return emptyList()
        val out = mutableListOf<String>()
        messagesNode.forEach { entry ->
            // Operation-level messages reference channel-level message keys, e.g.
            //   #/channels/<channelKey>/messages/<messageKey>
            // We resolve the trailing path segment and, where possible, map it to
            // the component-level message id by walking back through the parser.
            val ref = entry.get("\$ref")?.asText() ?: return@forEach
            val tail = ref.substringAfterLast('/')
            if (tail.isNotEmpty()) {
                out.add(tail)
            }
        }
        return out
    }

    private fun resolveLocalRef(node: JsonNode?, expectedPrefix: String): String? {
        if (node == null) return null
        val ref = node.get("\$ref")?.asText() ?: return null
        if (!ref.startsWith(expectedPrefix)) return null
        return ref.removePrefix(expectedPrefix)
    }

    private fun readFromRemoteServer(url: String): String {
        val response = connectToServer(url, attempts = 10)
        val body = response.readEntity(String::class.java)
        if (response.statusInfo.family != Response.Status.Family.SUCCESSFUL) {
            throw SutProblemException(
                "Cannot retrieve AsyncAPI schema from $url , status=${response.status} , body: $body"
            )
        }
        return body
    }

    private fun readFromDisk(asyncApiLocation: String): String {
        val path = try {
            if (asyncApiLocation.startsWith("file:", true)) {
                Paths.get(URI.create(asyncApiLocation))
            } else {
                Paths.get(asyncApiLocation)
            }
        } catch (e: Exception) {
            throw SutProblemException(
                "The path provided for the AsyncAPI schema $asyncApiLocation is invalid: ${e.message}"
            )
        }
        if (!Files.exists(path)) {
            throw SutProblemException("The provided AsyncAPI file does not exist: $asyncApiLocation")
        }
        return path.toFile().readText()
    }

    private fun connectToServer(url: String, attempts: Int): Response {
        for (i in 0 until attempts) {
            try {
                return ClientBuilder.newClient()
                    .target(url)
                    .request("*/*")
                    .get()
            } catch (e: Exception) {
                if (e.cause is ConnectException) {
                    Thread.sleep(1_000)
                } else {
                    throw SutProblemException("Failed to connect to $url: ${e.message}")
                }
            }
        }
        throw SutProblemException("Check the schema URL. Failed to connect to $url")
    }
}
